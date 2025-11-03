package perso.arcade.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import perso.arcade.model.Coin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class CoinService {

    private static final String BASE_URL = "https://www.monnaiedeparis.fr";
    private static final int TIMEOUT = 10000;
    private static final int MAX_PAGES = 20;
    private static final int THREADS = 5;

    public List<Coin> getCoins() {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Coin> allCoins = new ArrayList<>();

        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                Document doc = loadPage(page);
                if (doc == null) break;

                List<Element> products = extractProductElements(doc);
                if (products.isEmpty()) break;

                allCoins.addAll(scrapeProductsConcurrently(products, executor));
            }
        } finally {
            shutdownExecutor(executor);
        }

        return allCoins;
    }

    private Document loadPage(int page) {
        try {
            String url = BASE_URL + "/fr/monnaies?matiere=1252&p=" + page;
            return Jsoup.connect(url)
                    .timeout(TIMEOUT)
                    .get();
        } catch (IOException e) {
            System.out.println("❌ Impossible de charger la page " + page);
            return null;
        }
    }

    private List<Element> extractProductElements(Document doc) {
        Elements elements = doc.select(".product-item");
        return new ArrayList<>(elements);
    }

    private List<Coin> scrapeProductsConcurrently(List<Element> products, ExecutorService executor) {
        List<Future<Coin>> futures = new ArrayList<>();
        for (Element product : products) {
            futures.add(executor.submit(() -> scrapeProduct(product)));
        }

        List<Coin> coins = new ArrayList<>();
        for (Future<Coin> future : futures) {
            try {
                Coin coin = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
                if (coin != null) coins.add(coin);
            } catch (Exception e) {
                System.out.println("⚠️ Erreur lors du scraping d'une pièce : " + e.getMessage());
            }
        }
        return coins;
    }

    private Coin scrapeProduct(Element productHtml) {
        try {
            Coin coin = new Coin();
            coin.nom = getText(productHtml, ".product-item-name");
            coin.description = getDescription(productHtml);
            coin.statut = getStatut(productHtml);
            coin.prix = getPrix(productHtml);
            coin.link = getLink(productHtml);

            setPoids(coin);
            return coin;

        } catch (Exception e) {
            System.out.println("Erreur lors du parsing d'une pièce: " + e.getMessage());
            return null;
        }
    }

    private String getText(Element element, String selector) {
        return element.select(selector).text().trim();
    }

    private String getDescription(Element element) {
        return element.select(".product-item-link span")
                .text()
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String getStatut(Element element) {
        String status = element.select(".unavailable, .out-of-stock").text().trim();
        return status.isEmpty() ? "Disponible" : status;
    }

    private float getPrix(Element element) {
        String priceText = element.select(".price").text()
                .replaceAll("[^0-9.,]", "")
                .replace(",", ".");
        try {
            return Float.parseFloat(priceText);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    private String getLink(Element element) {
        String link = element.select(".product-item-link").attr("href");
        return link.startsWith("http") ? link : BASE_URL + link;
    }

    private void setPoids(Coin coin) {
        try {
            Document prodDoc = Jsoup.connect(coin.link).timeout(TIMEOUT).get();

            String poidsTexte = prodDoc.select("td[data-th=Poids]").text()
                    .replaceAll("[^0-9.,]", "")
                    .replace(",", ".");

            coin.poids = Float.parseFloat(poidsTexte);
            coin.prixParGramme = coin.poids > 0 ? coin.prix / coin.poids : 0.0f;

        } catch (Exception e) {
            coin.poids = 0.0F;
            coin.prixParGramme = 0.0F;
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("Tâches trop longues, arrêt forcé.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.out.println("Attente interrompue : " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
