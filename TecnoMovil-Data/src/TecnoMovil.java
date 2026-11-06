import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class TecnoMovil {
    // Un record guarda valores fijos. El enum limita las acciones válidas.
    enum Accion { ENTRADA, SALIDA }
    record Registro(int idUsuario, String ruta, String estacion,
                    Accion accion, LocalDateTime timestamp) {}

    // Cada viaje tiene una entrada y una salida. Cada usuario hace ida y regreso.
    static List<Registro> simular(int viajes) {
        var dia = LocalDateTime.of(2026, 11, 2, 0, 0);
        return IntStream.range(0, viajes).boxed().flatMap(i -> {
            int usuario = i / 2 + 1;
            boolean ida = i % 2 == 0;
            boolean norte = usuario % 3 != 0;
            String ruta = norte ? "R1" : "R2";
            String barrio = norte ? "Norte" : "Sur";
            var inicio = dia.withHour(ida ? 7 : 17)
                            .plusMinutes((usuario - 1) % 30);
            var entrada = new Registro(usuario, ruta, ida ? barrio : "Centro",
                                       Accion.ENTRADA, inicio);
            var salida = new Registro(usuario, ruta, ida ? "Centro" : barrio,
                                      Accion.SALIDA, inicio.plusMinutes(norte ? 12 : 18));
            return Stream.of(entrada, salida);
        }).sorted(Comparator.comparing(Registro::timestamp)).toList();
    }

    // Recibe una función que elige la clave: estación, hora o ruta.
    static <K> Map<K, Long> contar(Stream<Registro> datos,
                                   Function<Registro, K> clave) {
        return Map.copyOf(datos.collect(groupingBy(clave, counting())));
    }

    static boolean esEntrada(Registro r) {
        return r.accion() == Accion.ENTRADA;
    }

    // a) Cada validación de entrada cuenta como un ingreso.
    static Map<String, Long> afluencia(Stream<Registro> datos) {
        return contar(datos.filter(TecnoMovil::esEntrada), Registro::estacion);
    }

    // b y c) Conserva todos los empates y ordena sus claves para presentarlas.
    static <K extends Comparable<K>> List<K> mayores(Map<K, Long> conteo) {
        long maximo = conteo.values().stream().mapToLong(n -> n).max().orElse(0);
        return conteo.keySet().stream().filter(k -> conteo.get(k) == maximo)
                     .sorted().toList();
    }

    // Agrupa sin alterar el orden de llegada dentro de cada usuario.
    static Map<Integer, List<Registro>> porUsuario(Stream<Registro> datos) {
        return Map.copyOf(datos.collect(groupingBy(Registro::idUsuario,
                          collectingAndThen(toList(), List::copyOf))));
    }

    // d) Conserva también las visitas repetidas y devuelve listas de solo lectura.
    static Map<Integer, List<String>> patrones(Map<Integer, List<Registro>> grupos) {
        return grupos.entrySet().stream().collect(toUnmodifiableMap(
            e -> e.getKey(),
            e -> e.getValue().stream().map(Registro::estacion).toList()));
    }

    // e) Solo se mide una entrada seguida de una salida del mismo viaje.
    static LongStream segundosDeViaje(List<Registro> eventos) {
        var orden = eventos.stream().sorted(Comparator.comparing(Registro::timestamp))
                           .toList();
        return IntStream.range(1, orden.size()).mapToLong(i -> {
            var desde = orden.get(i - 1);
            var hasta = orden.get(i);
            boolean tramo = esEntrada(desde) && !esEntrada(hasta)
                && desde.ruta().equals(hasta.ruta())
                && !desde.estacion().equals(hasta.estacion());
            return tramo ? Duration.between(desde.timestamp(), hasta.timestamp())
                                   .getSeconds() : -1;
        }).filter(segundos -> segundos >= 0);
    }

    // Un bloque resume el cambio total y la mayor ocupación alcanzada desde cero.
    record Balance(long total, long pico) {
        Balance unir(Balance siguiente) {
            // El siguiente bloque comienza con el saldo que dejó el primero.
            return new Balance(total + siguiente.total,
                               Math.max(pico, total + siguiente.pico));
        }
    }

    // f) TreeMap ordena los instantes. En un mismo instante se cuenta el saldo neto.
    static long pico(List<Registro> eventos) {
        var cambios = eventos.stream().collect(groupingBy(Registro::timestamp,
            TreeMap::new, summingLong(r -> esEntrada(r) ? 1L : -1L)));
        return cambios.values().stream()
            .map(cambio -> new Balance(cambio, Math.max(0L, cambio)))
            .reduce(new Balance(0, 0), Balance::unir).pico();
    }

    // Se pueden repartir rutas entre hilos. Cada ruta calcula su propio pico.
    static Map<String, Long> ocupaciones(Stream<Registro> datos) {
        return Map.copyOf(datos.collect(groupingBy(Registro::ruta,
                          collectingAndThen(toList(), TecnoMovil::pico))));
    }

    // Esta clase solo coordina la ejecución y muestra los resultados.
    // Argumentos opcionales: cantidad de viajes, umbral y modo de ejecución.
    public static void main(String[] args) {
        int viajes = args.length > 0 ? Integer.parseInt(args[0]) : 12;
        int umbral = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        String modo = args.length > 2 ? args[2] : "secuencial";
        if (viajes < 0 || umbral < 0 || args.length > 3
                || !List.of("secuencial", "paralelo").contains(modo)) {
            throw new IllegalArgumentException("Use: viajes>=0 umbral>=0 secuencial|paralelo");
        }
        var datos = simular(viajes);
        // Cada operación necesita un stream nuevo porque solo se recorre una vez.
        java.util.function.Supplier<Stream<Registro>> flujo = modo.equals("paralelo")
            ? datos::parallelStream : datos::stream;
        var estaciones = afluencia(flujo.get());
        var horas = contar(flujo.get(), r -> r.timestamp().getHour());
        var rutas = contar(flujo.get().filter(TecnoMovil::esEntrada), Registro::ruta);
        var grupos = porUsuario(flujo.get());
        var visitas = patrones(grupos);
        var tiempos = grupos.values().stream().flatMapToLong(TecnoMovil::segundosDeViaje)
                            .summaryStatistics();
        var picos = ocupaciones(flujo.get());
        var criticas = picos.keySet().stream().filter(r -> picos.get(r) > umbral)
                            .sorted().toList();

        System.out.println("TecnoMovil Data | " + modo + " | registros: " + datos.size());
        System.out.println("Ingresos por estación: " + new TreeMap<>(estaciones));
        System.out.println("Eventos por hora: " + new TreeMap<>(horas));
        System.out.println("Horas pico: " + mayores(horas));
        System.out.println("Ingresos por ruta: " + new TreeMap<>(rutas));
        System.out.println("Rutas más usadas: " + mayores(rutas));
        System.out.println("Patrones calculados: " + visitas.size() + " (muestra de 2)");
        visitas.keySet().stream().sorted().limit(2).forEach(id ->
            System.out.println("  Usuario " + id + ": " + visitas.get(id)));
        System.out.printf(Locale.ROOT, "Tramos: %d | Promedio: %s%n", tiempos.getCount(),
            tiempos.getCount() == 0 ? "sin datos"
            : String.format(Locale.ROOT, "%.2f min", tiempos.getAverage() / 60));
        System.out.println("Pico de ocupación por ruta: " + new TreeMap<>(picos));
        System.out.println("Rutas críticas (más de " + umbral + "): " + criticas);
    }
}
