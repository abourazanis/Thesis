package thesis.drmReader.db;

import thesis.drmReader.db.EpubsContract.Authors;
import thesis.drmReader.db.EpubsContract.Epubs;

public enum EpubSorting {
    ALPHABETIC_ASC(0, Epubs.TITLE + " asc"), ALPHABETIC_DESC(1, Epubs.TITLE + " desc"), AUTHOR_ASC(2, Authors.LASTNAME + " asc"),
    AUTHOR_DESC(3, Authors.LASTNAME + " desc");
    private final int index;

    private final String query;

    EpubSorting(int index, String query) {
        this.index = index;
        this.query = query;
    }

    public int index() {
        return index;
    }

    public String query() {
        return query;
    }
}
