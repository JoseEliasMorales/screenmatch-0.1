package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=47e4ec4e";
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series;
    Optional<Serie> serieBuscada;

    public Principal(SerieRepository serieRepository) {
        this.repositorio = serieRepository;

    }


    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar series 
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar series por titulo
                    5 - Mostrar Top 5 Series 
                    6 - Buscar por categoria
                    7 - Buscar series por maximo de temporadas y evaluacion    
                    8 - Buscar por nombre de episodio       
                    9 - Top 5 Episodios por serie
                                          
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarTop5MejoresSeries();
                    break;
                case 6:
                    buscarPorcategoria();
                    break;
                case 7:
                    buscarPorMaximasTemporadasYPorEvaluacion();
                    break;
                case 8:
                    buscarPorNombreDeEpisodio();
                    break;
                case 9:
                    buscarTopEpisodiosPorSerie();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }

    }



    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }
    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("Escribe el nombre de la serie de la cual quieres ver los episodios: ");

        var nombreSeries = teclado.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(t-> t.getTitulo().toLowerCase().contains(nombreSeries.toLowerCase()))
                .findFirst();

        if(serie.isPresent()){
            var serieEncontrada = serie.get();

            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d->d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }




    }
    private void buscarSerieWeb() {
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        repositorio.save(serie);
        System.out.println(datos);
    }

    private void mostrarSeriesBuscadas() {
        series = repositorio.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getEvaluacion).reversed())
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Que serie quieres buscar? ");
        var nombreSerie = teclado.nextLine();

         serieBuscada = repositorio.findByTituloContainsIgnoreCase(nombreSerie);

        if(serieBuscada.isPresent()){
            System.out.println("La serie buscada es: " + serieBuscada.get());
        }else{
            System.out.println("No hemos encontrado ninguna serie");
        }
    }

    private void buscarTop5MejoresSeries(){
        List<Serie> serie = repositorio.findTop5ByOrderByEvaluacionDesc();
        serie.forEach(s ->
                System.out.println("Serie: " + s.getTitulo() + ", Evaluacion: " + s.getEvaluacion()));
    }

    private void buscarPorcategoria(){

        System.out.println("Escribe una categoria: ");

        var nombreCategoria = teclado.nextLine();
        var categoria=Categoria.fromEspanol(nombreCategoria);
        List<Serie> series = repositorio.findByGenero(categoria);

        series.forEach(System.out::println);

    }

    private void buscarPorMaximasTemporadasYPorEvaluacion(){
        System.out.println("Por favor, indica cuantas temporadas maximo quieres que tengan las series: ");
        var maximoTemporadas = teclado.nextInt();
        teclado.nextLine();

        System.out.println("Por favor, ingresa la evaluacion minima buscada: ");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> series = repositorio.seriesPorTemporadaYEvaluacion(maximoTemporadas, evaluacion);
        series.forEach(System.out::println);
    }

    private void buscarPorNombreDeEpisodio(){
        System.out.println("Por favor, escribe el nombre del episodio: ");
        var nombreEpisodio = teclado.nextLine();

        List<Episodio> episodioEncontrado = repositorio.episodiosPorNombre(nombreEpisodio);
        episodioEncontrado.forEach(e -> System.out.printf("Serie: %s Temporada: %s Episodio: %s Titulo: %s Evaluacion %s", e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo(), e.getEvaluacion()));
    }

    private void buscarTopEpisodiosPorSerie(){
        buscarSeriePorTitulo();

        if (serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = repositorio.top5episodios(serie);

            topEpisodios.forEach(e ->
                    System.out.printf("Serie: %s Temporada: %s Episodio: %s Titulo: %s Evaluacion: %s \n", e.getSerie().getTitulo(), e.getTemporada(),  e.getNumeroEpisodio(), e.getTitulo(), e.getEvaluacion()));
        }

    }
}

