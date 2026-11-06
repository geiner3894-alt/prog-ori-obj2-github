# TecnoMovil Data
Actividad evaluativa de Programación Orientada a Objetos II. El programa analiza entradas y salidas de un día simulado en TecnoValle utilizando la biblioteca estándar de Java.

## Ejecutar
Instala un JDK 17 o superior. Las evidencias se obtuvieron con OpenJDK 21. Abre una terminal en esta carpeta (la que contiene src) y ejecuta:

```shell
java src/TecnoMovil.java 12 3 secuencial
java src/TecnoMovil.java 100000 10000 paralelo
```

Los argumentos indican el número de viajes, el umbral de pasajeros simultáneos por ruta y el modo. Sin argumentos se usan 12 viajes, umbral 3 y modo secuencial. Cada viaje genera dos registros. Para comprobar la igualdad de resultados, ejecuta la segunda orden cambiando paralelo por secuencial.

## Interpretar el resultado
Las estaciones y rutas cuentan entradas. Las horas pico cuentan entradas y salidas, incluidos empates. El programa conserva cada estación en el historial del usuario, aunque se repita. La duración se calcula entre una entrada y la siguiente salida del mismo usuario y ruta en otra estación. Para la ocupación se parte de cero y se suman los cambios netos de cada instante. Una ruta es crítica si supera el umbral.

## Archivos
- src/TecnoMovil.java contiene el código completo.
- evidencias contiene las dos capturas de ejecución.
- informe contiene el informe editable en Word. Para entregar, completa los datos de portada y los enlaces del grupo, exporta el PDF con el nombre indicado por la asignatura y súbelo a esta carpeta de la rama main. Cada integrante debe entregar el PDF en la plataforma.
