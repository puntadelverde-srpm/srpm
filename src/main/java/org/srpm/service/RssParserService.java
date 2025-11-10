package org.srpm.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.srpm.dao.NoticiaDAO;
import org.srpm.model.Noticia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @Service indica a Spring que esta clase es un "Servicio".
 */
@Service
public class RssParserService {

    /**
     * **CAMBIO:** Se ELIMINA la constante LIMITE_NOTICIAS_POR_FEED,
     * ya que ahora se recibirá como parámetro desde el controlador.
     */
    // private static final int LIMITE_NOTICIAS_POR_FEED = 5;

    /**
     * Clase interna privada para almacenar el nombre y la URL de un feed.
     */
    private static class FeedSource {
        private final String nombre;
        private final String url;

        public FeedSource(String nombre, String url) {
            this.nombre = nombre;
            this.url = url;
        }

        public String getNombre() { return nombre; }
        public String getUrl() { return url; }
    }

    /**
     * Aquí definimos la lista de todas las fuentes RSS que queremos leer.
     */
    private final List<FeedSource> feeds = List.of(
            new FeedSource("20minutos", "https://www.20minutos.es/rss/"),
            new FeedSource("COPE", "https://www.cope.es/api/es/news/rss.xml"),
            new FeedSource("elDiario", "https://www.eldiario.es/rss/")
    );

    private final NoticiaDAO noticiaDAO;

    @Autowired
    public RssParserService(NoticiaDAO noticiaDAO) {
        this.noticiaDAO = noticiaDAO;
    }

    /**
     * Método principal que llama la API.
     * **MODIFICACIÓN:** Acepta el límite como parámetro.
     */
    public String fetchAllFeeds(int limiteNoticiasPorFeed) {
        System.out.println("--- [PETICIÓN API] Iniciando lectura de RSS con límite: " + limiteNoticiasPorFeed + " ---");
        int totalNuevas = 0;

        for (FeedSource feed : feeds) {
            // Se pasa el límite al método de parsing
            int nuevas = parseAndSaveFeed(feed.getUrl(), feed.getNombre(), limiteNoticiasPorFeed);
            System.out.println("Fuente: " + feed.getNombre() + " - Noticias nuevas: " + nuevas);
            totalNuevas += nuevas;
        }

        String resumen = "Actualización completada. " + totalNuevas + " noticias nuevas añadidas.";
        System.out.println("--- [PETICIÓN API] " + resumen + " ---");
        return resumen;
    }

    /**
     * Lee una URL de RSS, la procesa y guarda las noticias nuevas en la BBDD.
     * **MODIFICACIÓN:** Acepta el límite como parámetro.
     */
    private int parseAndSaveFeed(String feedUrl, String sourceName, int limiteNoticiasPorFeed) {
        int noticiasNuevasContador = 0;

        try (var reader = new XmlReader(new URL(feedUrl))) {

            SyndFeed feed = new SyndFeedInput().build(reader);
            List<SyndEntry> todasLasNoticiasDelFeed = feed.getEntries();

            // Aplicar el límite
            int totalNoticiasEnFeed = todasLasNoticiasDelFeed.size();
            // **MODIFICACIÓN:** Usar el parámetro en lugar de la constante
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