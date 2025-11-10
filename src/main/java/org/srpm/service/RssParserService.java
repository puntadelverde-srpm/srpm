package org.srpm.service;

import com.rometools.rome.feed.synd.SyndEntry;
// ... (otras importaciones)
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
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

@Service
public class RssParserService {

    /**
     * **CAMBIO MINIMO:** Se ELIMINA la constante, ya que se recibirá por parámetro.
     */
    // private static final int LIMITE_NOTICIAS_POR_FEED = 5;

    // ... (Clase interna FeedSource y lista 'feeds' se mantienen igual) ...
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
     * **CAMBIO MINIMO:** Se añade `limiteNoticiasPorFeed` como parámetro.
     */
    public String fetchAllFeeds(int limiteNoticiasPorFeed) {
        System.out.println("--- [PETICIÓN API] Iniciando lectura de RSS con límite: " + limiteNoticiasPorFeed + " ---");
        int totalNuevas = 0;

        for (FeedSource feed : feeds) {
            // **CAMBIO MINIMO:** Se pasa el límite al método privado.
            int nuevas = parseAndSaveFeed(feed.getUrl(), feed.getNombre(), limiteNoticiasPorFeed);
            System.out.println("Fuente: " + feed.getNombre() + " - Noticias nuevas: " + nuevas);
            totalNuevas += nuevas;
        }

        String resumen = "Actualización completada. " + totalNuevas + " noticias nuevas añadidas.";
        System.out.println("--- [PETICIÓN API] " + resumen + " ---");
        return resumen;
    }

    /**
     * **CAMBIO MINIMO:** Se añade `limiteNoticiasPorFeed` como parámetro.
     */
    private int parseAndSaveFeed(String feedUrl, String sourceName, int limiteNoticiasPorFeed) {
        int noticiasNuevasContador = 0;

        try (var reader = new com.rometools.rome.io.XmlReader(new URL(feedUrl))) {

            SyndFeed feed = new SyndFeedInput().build(reader);
            List<SyndEntry> todasLasNoticiasDelFeed = feed.getEntries();

            // Aplicar el límite
            int totalNoticiasEnFeed = todasLasNoticiasDelFeed.size();
            // **CAMBIO MINIMO:** Se usa el parámetro en lugar de la constante.
            int limiteReal = Math.min(totalNoticiasEnFeed, limiteNoticiasPorFeed);
            List<SyndEntry> noticiasLimitadas = todasLasNoticiasDelFeed.subList(0, limiteReal);

            // ... (El resto del método se mantiene igual) ...
            System.out.println("Fuente: " + sourceName + " - Encontradas " + totalNoticiasEnFeed + ", procesando " + limiteReal);

            for (SyndEntry entry : noticiasLimitadas) {
                // ... (lógica de guardado se mantiene igual) ...
            }

        } catch (Exception e) {
            System.err.println("Error parseando el feed: " + feedUrl);
            e.printStackTrace();
        }

        return noticiasNuevasContador;
    }

    // ... (Los métodos 'extraerContenido' y 'limpiarHtml' se mantienen totalmente igual) ...

    private String extraerContenido(SyndEntry entry) {
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            return entry.getContents().get(0).getValue();
        }
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue();
        }
        return null;
    }

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