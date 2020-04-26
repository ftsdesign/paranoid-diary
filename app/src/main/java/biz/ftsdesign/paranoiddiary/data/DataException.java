package biz.ftsdesign.paranoiddiary.data;

/**
 * To report critical problems with the data layer, not related to security.
 */
public class DataException extends Exception {
    DataException(String message) {
        super(message);
    }
}
