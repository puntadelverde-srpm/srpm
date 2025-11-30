package org.srpm.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import jakarta.annotation.PostConstruct;
import org.srpm.dao.NoticiaDAO;
import org.srpm.model.Noticia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URL;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;


@Service
public class RssParserService {

    // 1. Inyectamos las URLs desde el archivo de propiedades
    @Value("${rss.url.20minutos}")
    private String url20Minutos;

    @Value("${rss.url.cope}")
    private String urlCope;

    @Value("${rss.url.eldiario}")
    private String urlElDiario;

    /**
     * Esta es la lista de fuentes.
     * IMPORTANTE: No la inicializamos aquí, porque las URLs de arriba
     * todavía serían 'null'. La inicializaremos en el método @PostConstruct.
     */
    private List<FeedSource> feeds;

    private final NoticiaDAO noticiaDAO;

    //Record para FeedSource
    private record FeedSource(String nombre, String url) { }


    @Autowired
    public RssParserService(NoticiaDAO noticiaDAO) {
        this.noticiaDAO = noticiaDAO;
        System.out.println(": RssParserService CONSTRUIDO. Las URLs aún son null.");
    }

    @PostConstruct
    public void inicializarFuentes() {
        System.out.println("============================================================");
        System.out.println("DEBUG: Ejecutando @PostConstruct para cargar URLs...");
        System.out.println("DEBUG - 20minutos: " + url20Minutos);
        System.out.println("DEBUG - COPE: " + urlCope);
        System.out.println("DEBUG - elDiario: " + urlElDiario);

        // 2. Ahora sí creamos la lista, porque las URLs ya no son null
        this.feeds = List.of(
                new FeedSource("20minutos", url20Minutos),
                new FeedSource("COPE", urlCope),
                new FeedSource("elDiario", urlElDiario)
        );

        System.out.println("DEBUG: ¡Lista de 'feeds' creada! (" + feeds.size() + " fuentes)");
        System.out.println("============================================================");
    }


    /**
     * Método principal que llama la API.
     * (Este método no cambia)
     */
    public String fetchAllFeeds(int limiteNoticiasPorFeed) {
        System.out.println("--- [PETICIÓN API] Iniciando lectura de RSS con límite: " + limiteNoticiasPorFeed + " ---");
        int totalNuevas = 0;

        // Comprobación por si algo falló en PostConstruct
        if (feeds == null || feeds.isEmpty()) {
            System.err.println("¡¡ERROR GRAVE!! La lista de feeds está vacía. Revisa @PostConstruct y application.properties.");
            return "Error: Lista de feeds no inicializada.";
        }

        for (FeedSource feed : feeds) {
            // Se pasa el límite al método de parsing
            int nuevas = parseAndSaveFeed(feed.url(), feed.nombre(), limiteNoticiasPorFeed);
            System.out.println("Fuente: " + feed.nombre() + " - Noticias nuevas: " + nuevas);
            totalNuevas += nuevas;
        }

        String resumen = "Actualización completada. " + totalNuevas + " noticias nuevas añadidas.";
        System.out.println("--- [PETICIÓN API] " + resumen + " ---");
        return resumen;
    }

    /**
     * Lee una URL de RSS, la procesa y guarda las noticias nuevas en la BBDD.
     * (Este método no cambia)
     */
    private int parseAndSaveFeed(String feedUrl, String sourceName, int limiteNoticiasPorFeed) {
        int noticiasNuevasContador = 0;

        // feedUrl ya no será 'null' gracias al @PostConstruct
        try (var reader = new XmlReader(new URL(feedUrl))) {

            SyndFeed feed = new SyndFeedInput().build(reader);
            List<SyndEntry> todasLasNoticiasDelFeed = feed.getEntries();

            // Aplicar el límite
            int totalNoticiasEnFeed = todasLasNoticiasDelFeed.size();
            int limiteReal = Math.min(totalNoticiasEnFeed, limiteNoticiasPorFeed);
            List<SyndEntry> noticiasLimitadas = todasLasNoticiasDelFeed.subList(0, limiteReal);

            System.out.println("Fuente: " + sourceName + " - Encontradas " + totalNoticiasEnFeed + ", procesando " + limiteReal);

            for (SyndEntry entry : noticiasLimitadas) {

                String link = entry.getLink();
                if (link == null) {
                    continue;
                }

                // Evitar duplicados
                if (noticiaDAO.findByLinkNoticia(link).isEmpty()) {

                    noticiasNuevasContador++;

                    Noticia nuevaNoticia = new Noticia();
                    nuevaNoticia.setLinkNoticia(link);
                    nuevaNoticia.setFuente(sourceName);

                    // Limpiamos el titular
                    nuevaNoticia.setTitular(limpiarHtml(entry.getTitle()));

                    // Extraemos y limpiamos el contenido
                    String contenidoHtml = extraerContenido(entry);
                    nuevaNoticia.setContenido(limpiarHtml(contenidoHtml));

                    // Convertir la fecha
                    if (entry.getPublishedDate() != null) {
                        nuevaNoticia.setFecha(entry.getPublishedDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime());
                    }

                    noticiaDAO.save(nuevaNoticia);
                }
            }

        } catch (Exception e) {
            System.err.println("Error parseando el feed: " + feedUrl);
            e.printStackTrace();
        }

        return noticiasNuevasContador;
    }

    /**
     * Método de ayuda (privado) para encontrar el contenido de la noticia.
     * (Este método no cambia)
     */
    private String extraerContenido(SyndEntry entry) {
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            return entry.getContents().get(0).getValue();
        }
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue();
        }
        return null;
    }


    /**
     * Método de ayuda para limpiar el HTML y otros caracteres no deseados.
     * (Este método no cambia)
     */
    private String limpiarHtml(String html) {
        if (html == null) {
            return null;
        }

        String texto = HtmlUtils.htmlUnescape(html);

        Pattern scriptPattern = Pattern.compile("<script[^>]*?>.*?</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        texto = scriptPattern.matcher(texto).replaceAll("");

        Pattern stylePattern = Pattern.compile("<style[^>]*?>.*?</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        texto = stylePattern.matcher(texto).replaceAll("");

        texto = texto.replaceAll("<[^>]*>", "");
        texto = texto.replaceAll("[\\r\\n]+", " ");
        texto = texto.replaceAll("\\|", " ");
        texto = texto.replaceAll("\"", "");
        texto = texto.replaceAll("\\\\", "");
        texto = texto.replaceAll(" +", " ");

        return texto.trim();
    }
}