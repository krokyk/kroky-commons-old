package org.kroky.common.utils;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Kroky
 */
public class Utils {

    private static final Logger LOG = LogManager.getLogger();
    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final NumberFormat TWO_DECIMAL_FORMAT = NumberFormat.getNumberInstance(Locale.ENGLISH);

    static {
        //setup the 2-decimal formatter
        TWO_DECIMAL_FORMAT.setMinimumFractionDigits(2);
        TWO_DECIMAL_FORMAT.setMaximumFractionDigits(2);
    }

    private static final int URL_READ_MAX_ATTEMPTS = 3;
    private static final Map<String, String> HTML_TEXTS = new HashMap<String, String>();
    private static final Map<String, String> HTML_SOURCES = new HashMap<String, String>();

    public static String formatDateTime(Object date) {
        return DATE_TIME_FORMAT.format(date);
    }

    public static String formatDate(Object date) {
        return DATE_FORMAT.format(date);
    }

    public static Timestamp stringToTimestamp(String dateStr) {
        try {
            return new Timestamp(DATE_TIME_FORMAT.parse(dateStr).getTime());
        } catch (ParseException ex) {
            LOG.error("Unable to parse the given string: " + dateStr);
        }
        return null;
    }

    public static String mapToString(Map<?, ?> map) {
        StringBuilder sb;
        if (map == null) {
            sb = new StringBuilder("null");
        } else if (map.isEmpty()) {
            sb = new StringBuilder("empty");
        } else {
            sb = new StringBuilder();
            for (Object o : map.keySet()) {
                sb.append(o.toString()).append(" => ").append(map.get(o).toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public static boolean isEmpty(Object obj) {
        if (obj instanceof String) {
            String str = (String) obj;
            return str == null || str.length() == 0;
        }
        return obj == null;
    }

    public static String capitalizeFirstLetter(String str) {
        if (isEmpty(str)) {
            LOG.warn("Received null or empty string for first letter capitalization. Not capitalizing.");
            return str;
        }
        char capital = str.toUpperCase().charAt(0);
        return capital + str.substring(1);
    }

//    public static void showError(Component parent, Throwable t) {
//        StringBuilder sb = new StringBuilder();
//        String title;
//        if(t instanceof BettingException) {
//            sb.append(((BettingException)t).getUserFriendlyMessage());
//            title = "Error";
//        } else if (t instanceof BettingRuntimeException) {
//            sb.append(((BettingRuntimeException)t).getUserFriendlyMessage());
//            title = "Runtime Error";
//        } else {
//            sb.append(t.getMessage());
//            title = "Unexpected Error";
//        }
//        sb.append("\nPlease see error log for details");
//
//        JOptionPane.showMessageDialog(parent, sb.toString(), title, JOptionPane.ERROR_MESSAGE);
//    }
    /**
     * Same as getHtmlFromUrl(url, false);
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static String getHtmlFromUrl(String url) {
        return getHtmlFromUrl(url, false);
    }

    /**
     * Gets the whole HTML source from the url.
     *
     * @param url
     * @param cached if true, attempts to retrieve data from cache, if not found goes to web
     * @return
     * @throws Exception
     */
    public static String getHtmlFromUrl(String url, boolean cached) {
        InputStreamReader isr = null;
        int readStreamFailedCount = 0;
        while (readStreamFailedCount < URL_READ_MAX_ATTEMPTS) {
            try {
                if (cached && HTML_SOURCES.containsKey(url)) {
                    return HTML_SOURCES.get(url);
                }
                // Read all the text returned by the server
                // try several times
                int openStreamFailedCount = 0;
                LOG.info("Trying to open input stream from URL: " + url);
                while (openStreamFailedCount < URL_READ_MAX_ATTEMPTS) {
                    try {
                        isr = new InputStreamReader(new URL(url).openStream(), "UTF-8");
                        openStreamFailedCount = URL_READ_MAX_ATTEMPTS;
                        LOG.info("Success, trying to read from it...");
                    } catch (IOException e) {
                        openStreamFailedCount++;
                        if (openStreamFailedCount == URL_READ_MAX_ATTEMPTS) {
                            throw e;
                        }
                        LOG.error("Failure, trying again...");
                    }
                }
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = isr.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                HTML_SOURCES.put(url, sb.toString());
                LOG.info("Success, returning content");
                return sb.toString();
            } catch (Exception e) {
                readStreamFailedCount++;
                if (readStreamFailedCount == URL_READ_MAX_ATTEMPTS) {
                    String message = "Failed to read from URL: " + url;
                    throw new RuntimeException(message + "\nNumber of attempts: " + readStreamFailedCount, e);
                }
                LOG.error("Failure, trying again...");
            } finally {
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (IOException e) {
                        //nothing to do
                    }
                }
            }
        }
        return null;
    }

    /**
     * Same as getTextFromUrl(url, false);
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static String getTextFromUrl(String url) {
        return getTextFromUrl(url, false);
    }

    /**
     * Gets the whole HTML source from the url, stripped off of tags.
     *
     * @param url
     * @param cached if true, attempts to retrieve data from cache, if not found goes to web
     * @return
     * @throws Exception
     */
    public static String getTextFromUrl(String url, boolean cached) {
        if (cached && HTML_TEXTS.containsKey(url)) {
            return HTML_TEXTS.get(url);
        }
        // Read all the text returned by the server
        String text = getHtmlFromUrl(url, cached);
        //remove tags from it and replace with TABS
        text = text.replaceAll("<[^>]+>", "\t");
        HTML_TEXTS.put(url, text);
        return text;
    }

    public static void centerOnParent(Component parent, Component child) {
        if (parent == null) {
            if (child instanceof Window) {
                ((Window) child).setLocationRelativeTo(null);
            }
            return;
        }
        //p = parent
        //c = child
        int pX = parent.getLocation().x;
        int pY = parent.getLocation().y;

        Dimension pSize = parent.getSize();
        Dimension cSize = child.getSize();

        int cX = pX + (pSize.width - cSize.width) / 2;
        int cY = pY + (pSize.height - cSize.height) / 2;
        child.setLocation(cX, cY);
    }

    private static long previousMillis = 0;

    public static Timestamp now() {
        long currentMillis = System.currentTimeMillis();
        while (currentMillis <= previousMillis) {
            currentMillis++;
        }
        previousMillis = currentMillis;
        return new Timestamp(currentMillis);
    }

    public static Timestamp timestamp(Object obj) {
        if (obj instanceof Date) {
            return new Timestamp(((Date) obj).getTime());
        } else if (obj instanceof Timestamp) {
            return (Timestamp) obj;
        } else if (obj instanceof String) {
            return stringToTimestamp((String) obj);
        }
        return null;
    }

    public static String formatTwoDecimal(Object value) {
        return TWO_DECIMAL_FORMAT.format(value);
    }

    public static void prettyPrint(Object[] objects) {
        for (Object obj : objects) {
            System.out.println(obj.toString());
        }
    }

    /**
     * round value up if there is anything else than 0 in the second decimal place
     *
     * @param value
     * @return
     */
    public static double roundUp(double value) {
        return (double) Math.round((value + 0.04) * 10) / 10;
    }

    public static <T extends Comparable<? super T>> List<T> sortAsc(Collection<T> col) {
        List<T> list = new ArrayList<T>(col);
        Collections.sort(list);
        return list;
    }

    public static <T extends Comparable<? super T>> List<T> sortDesc(Collection<T> col) {
        List<T> list = sortAsc(col);
        Collections.reverse(list);
        return list;
    }

    private static Calendar c = Calendar.getInstance();

    public static Timestamp getTimestampAtDayPrecision(Timestamp date) {
        c.setTimeInMillis(date.getTime());
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        c.clear();
        c.set(year, month, day);
        return new Timestamp(c.getTimeInMillis());
    }

    public static File getFullFile(Object path) {
        if (path == null) {
            return null;
        }
        return getFullFile(path.toString());
    }

    public static File getFullFile(String path) {
        return new File(getFullPath(path));
    }

    public static File getFullFile(File file) {
        return new File(getFullPath(file));
    }

    public static String getFullPath(String path) {
        return getFullPath(new File(path));
    }

    public static String getFullPath(File file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException ex) {
            return file.getAbsolutePath();
        }
    }
}
