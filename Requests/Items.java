package Requests;

import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cip on 28-Feb-17.
 */
public class Items implements Serializable {
    private List<Item> content = new ArrayList<>();

    public List<Item> getContent() {
        return content;
    }

    public void addItem(Item item) {
        content.add(item);
    }

    public Item getItem(int index) {
        return content.get(index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < content.size(); ++i) {
            sb.append(i + 1).append(". ").append(content.get(i).toString()).append("\n");
        }

        return sb.toString();
    }

    public static final class Item implements Serializable {
        private String name;
        private int value;

        public Item(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name + ". Price: " + value + "$.";
        }
    }
}
