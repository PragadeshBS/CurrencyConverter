
// javac -cp ".;json.jar" Main.java && java -cp ".;json.jar" Main
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import org.json.*;

interface UserHandler {
    void greet();

    void menu();

    void exit();
}

class DataFetch {
    private String fetchURL;

    DataFetch(String url) {
        fetchURL = url;
    }

    private String _fetch(String urlToFetch) {
        try {
            URL url = new URL(urlToFetch);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.connect();
            String result = new BufferedReader(new InputStreamReader(request.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            return result;
        } catch (Exception e) {
            System.out.println("An error occured while trying to fetch data from " + fetchURL);
            return null;
        }
    }

    String fetch() {
        return _fetch(fetchURL);
    }

    String fetch(String url) {
        return _fetch(url);
    }
}

class DataFetchWithParameters extends DataFetch {
    private Map<String, String> params;
    private StringBuilder tempUrl;

    DataFetchWithParameters(String url) {
        super(url);
        tempUrl = new StringBuilder(url);
        params = new HashMap<>();
    }

    void addParameter(String key, String value) {
        if (params.size() == 0)
            tempUrl.append('?');
        else
            tempUrl.append('&');
        String query = key + "=" + value;
        tempUrl.append(query);
        params.put(key, value);
    }

    String fetchWithParams() {
        return fetch(tempUrl.toString());
    }
}

class CurrencyExchange {
    private JSONObject json;
    private JSONObject ratesJson;
    private List<String> currencyCodes;

    void fetchExchangeRates() {
        String latestRatesEndpoint = "https://api.exchangerate.host/latest";
        DataFetchWithParameters df = new DataFetchWithParameters(latestRatesEndpoint);
        df.addParameter("source", "imf");
        df.addParameter("base", "USD");
        String res = df.fetchWithParams();
        if (res != null) {
            json = new JSONObject(res);
            ratesJson = json.getJSONObject("rates");
            currencyCodes = new ArrayList<>(Arrays.asList(JSONObject.getNames(ratesJson)));
        }
    }

    void printLocalRate() {
        System.out.println("1 USD = " + ratesJson.getDouble("INR") + " INR");
    }

    String getDate() {
        return json.getString("date");
    }

    private boolean _isValidCurrencyCode(String currencyCode) {
        return currencyCodes.contains(currencyCode);
    }

    private double _convert(String c1, String c2, double amt) {
        double valueOfC1AmtInUSD = amt * 1 / ratesJson.getDouble(c1);
        double valueOfUSDInC2 = ratesJson.getDouble(c2);
        return valueOfC1AmtInUSD * valueOfUSDInC2;
    }

    void performConversion() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter currency code to convert from: ");
        String c1 = sc.next().toUpperCase();
        if (!_isValidCurrencyCode(c1)) {
            System.out.println("Invalid currency code");
            return;
        }
        System.out.print("Enter currency code to convert to: ");
        String c2 = sc.next().toUpperCase();
        if (!_isValidCurrencyCode(c2)) {
            System.out.println("Invalid currency code");
            return;
        }
        System.out.print("Enter amount to convert: ");
        double amt = sc.nextDouble();
        sc.nextLine();
        if (amt < 0) {
            System.out.println("Invalid amount");
            return;
        }
        double result = _convert(c1, c2, amt);
        String rounded = String.format("%.3f", result);
        System.out.println(amt + " " + c1 + " = " + rounded + " " + c2);
    }

    List<String> getCurrencyCodes() {
        return currencyCodes;
    }
}

class CurrencyUserHandler implements UserHandler {

    CurrencyExchange ce;

    public void greet() {
        System.out.println("...............\n");
        System.out.println("    Welcome    \n");
        System.out.println("...............\n");
        System.out.println("Date: " + ce.getDate());
    }

    public void menu() {
        Scanner in = new Scanner(System.in);
        String choice;
        while (true) {
            System.out.print("\nh for help >>");
            choice = in.next();
            if (choice.equalsIgnoreCase("e"))
                break;
            perform(choice);
        }
        in.close();
        exit();
    }

    public void exit() {
        System.out.println("Bye!");
    }

    private void printHelpMenu() {
        String[][] helpMenuItems = {
                { "c", "Convert" },
                { "co", "Print currency codes" },
                { "h", "Print this help menu" },
                { "f", "Fetch latest exchange rates" },
                { "e", "Exit" }
        };
        for (var item : helpMenuItems) {
            System.out.println(item[0] + ":\t" + item[1]);
        }
    }

    private void perform(String action) {
        if (action.equals("c")) {
            ce.performConversion();
        } else if (action.equals("co")) {
            for (String s : ce.getCurrencyCodes()) {
                try {
                    System.out.println(Currency.getInstance(s).getDisplayName() + " - " + s);
                } catch (IllegalArgumentException e) {
                    System.out.println(s);
                }
            }
        } else if (action.equals("h"))
            printHelpMenu();
        else if (action.equals("f")) {
            ce.fetchExchangeRates();
            System.out.println("Exchanges rates fetched succcessfully");
            System.out.println("Latest rate:");
            ce.printLocalRate();
        }
    }

    synchronized void fetchData() {
        ce = new CurrencyExchange();
        System.out.println("Fetching latest currency exchange rates...");
        ce.fetchExchangeRates();
        System.out.println("Data fetch complete, notifying blocked thread...");
        notify();
    }

    synchronized void init() {
        System.out.println("User I/O blocked. Waiting for data fetch from another thread...");
        try {
            wait();
            System.out.println("User I/O thread resuming...");
        } catch (Exception e) {
            System.out.println("Something went wrong");
        }
        System.out.println("Rates fetched successfully");
        greet();
        System.out.print("Today's Currency Exchange rate: ");
        ce.printLocalRate();
        System.out.println("\n***************");
        menu();
    }
}

public class Main {
    public static void main(String[] args) throws Exception {
        CurrencyUserHandler user = new CurrencyUserHandler();
        System.out.println("Starting thread to handle user I/O...");
        new Thread(new Runnable() {
            public void run() {
                user.init();
            }
        }).start();
        System.out.println("Starting thread to handle data fetch...");
        new Thread(new Runnable() {
            public void run() {
                user.fetchData();
            }
        }).start();
    }
}