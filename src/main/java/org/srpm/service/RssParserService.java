package org.srpm.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.srpm.dao.NoticiaDAO;
import org.srpm.model.Noticia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils; // Import para limpiar HTML

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern; // <--- ¡¡NUEVA IMPORTACIÓN NECESARIA!!

/**
 * @Service indica a Spring que esta clase es un "Servicio".
 * Es la clase encargada de la lógica de negocio (en este caso, leer RSS).
 * Spring creará un objeto (bean) de esta clase para que otros puedan usarla.
 */
@Service
public class RssParserService {

    /**
     * Límite máximo de noticias a comprobar por CADA feed.
     */
    private static final int LIMITE_NOTICIAS_POR_FEED = 20;

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

    // Necesitamos el DAO (Data Access Object) para interactuar con la Base de Datos.
    private final NoticiaDAO noticiaDAO;

    /**
     * @Autowired en el constructor (Inyección de Dependencias):
     */
    @Autowired
    public RssParserService(NoticiaDAO noticiaDAO) {
        this.noticiaDAO = noticiaDAO;
    }

    /**
     * Método principal que llama la API.
     */
    public String fetchAllFeeds() {
        System.out.println("--- [PETICIÓN API] Iniciando lectura de RSS ---");
        int totalNuevas = 0;

        for (FeedSource feed : feeds) {
            int nuevas = parseAndSaveFeed(feed.getUrl(), feed.getNombre());
            System.out.println("Fuente: " + feed.getNombre() + " - Noticias nuevas: " + nuevas);
            totalNuevas += nuevas;
        }

        String resumen = "Actualización completada. " + totalNuevas + " noticias nuevas añadidas.";
        System.out.println("--- [PETICIÓN API] " + resumen + " ---");
        return resumen;
    }

    /**
     * Lee una URL de RSS, la procesa y guarda las noticias nuevas en la BBDD.
     */
    private int parseAndSaveFeed(String feedUrl, String sourceName) {
        int noticiasNuevasContador = 0;

        try (var reader = new com.rometools.rome.io.XmlReader(new URL(feedUrl))) {

            SyndFeed feed = new SyndFeedInput().build(reader);
            List<SyndEntry> todasLasNoticiasDelFeed = feed.getEntries();

            // Aplicar el límite
            int totalNoticiasEnFeed = todasLasNoticiasDelFeed.size();
            int limiteReal = Math.min(totalNoticiasEnFeed, LIMITE_NOTICIAS_POR_FEED);
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
     * --- VERSIÓN MEJORADA ---
     */
    private String limpiarHtml(String html) {
        if (html == null) {
            return null;
        }

        // 1. Decodifica entidades HTML (ej. &nbsp; o &aacute;)
        String texto = HtmlUtils.htmlUnescape(html);

        // --- INICIO DE LA MODIFICACIÓN ---

        // 2. [NUEVO] Eliminar bloques de <script> completos (incluido el contenido)
        // Usamos Pattern.DOTALL para que '.' incluya saltos de línea,
        // y CASE_INSENSITIVE por si es <SCRIPT>
        Pattern scriptPattern = Pattern.compile("<script[^>]*?>.*?</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        texto = scriptPattern.matcher(texto).replaceAll("");

        // 3. [NUEVO] Eliminar bloques de <style> completos (incluido el contenido)
        Pattern stylePattern = Pattern.compile("<style[^>]*?>.*?</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        texto = stylePattern.matcher(texto).replaceAll("");

        // --- FIN DE LA MODIFICACIÓN ---

        // 4. Quita TODAS las etiquetas HTML restantes (ej. <b>, <p>, <img>)
        texto = texto.replaceAll("<[^>]*>", "");

        // 5. Quita saltos de línea (reemplaza por un espacio)
        texto = texto.replaceAll("[\\r\\n]+", " ");

        // 6. Quita barras verticales (reemplaza por un espacio)
        texto = texto.replaceAll("\\|", " ");

        // 7. Quita comillas dobles (")
        // Ojo: esto puede quitar comillas de frases, si prefieres no quitarlo, comenta la línea
        texto = texto.replaceAll("\"", "");

        // 8. Quita barras invertidas (\)
        texto = texto.replaceAll("\\\\", "");

        // 9. Reemplaza múltiples espacios por un solo espacio
        texto = texto.replaceAll(" +", " ");

        return texto.trim(); // Quita espacios en blanco al inicio/final
    }
}