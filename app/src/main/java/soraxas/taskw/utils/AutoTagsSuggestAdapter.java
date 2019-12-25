package soraxas.taskw.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AutoTagsSuggestAdapter extends ArrayAdapter {
    private Context context;
    private int resource;
    private List<String> items;
    private List<String> tempItems;
    private List<String> suggestions;

    public AutoTagsSuggestAdapter(Context context, int resource, List<String> items) {
        super(context, resource, 0, items);

        this.context = context;
        this.resource = resource;
        this.items = items;
        tempItems = new ArrayList<String>(items);
        suggestions = new ArrayList<String>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resource, parent, false);
        }

        String item = items.get(position);

        if (item != null && view instanceof TextView) {
            ((TextView) view).setText(item);
        }

        return view;
    }

    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    Filter nameFilter = new Filter() {
        @Override
        public CharSequence convertResultToString(Object resultValue) {
            String str = (String) resultValue;
            return str;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                suggestions.clear();
                String constraint_str = constraint.toString();
                boolean last_char_is_space = (constraint_str.length() > 0 &&
                        constraint_str.charAt(constraint_str.length() - 1) == ' ');

                List<String> tags = Arrays.asList(constraint_str.split(" "));
                String prefix;
                // if the last char is space, we will treat constraint as if it's a wildcard
                if (last_char_is_space) {
                    prefix = constraint_str;
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < tags.size() - 1; ++i) {
                        sb.append(tags.get(i));
                        sb.append(" ");
                    }
                    prefix = sb.toString();
                }

                // the actual term to search for
                String constraint_term = tags.get(tags.size() - 1).toLowerCase();
                Collections.sort(tempItems);
                for (String names : tempItems) {
//                    if (names.toLowerCase().contains(last_terms))
                    if (last_char_is_space || names.toLowerCase().startsWith(constraint_term)) {
                        // the term shouldn't already appear previously
                        if (!tags.contains(names)) {
                            suggestions.add(String.format("%s %s", prefix, names));
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            List<String> filterList = (ArrayList<String>) results.values;
            if (results != null && results.count > 0) {
                clear();
                for (String item : filterList) {
                    add(item);
                    notifyDataSetChanged();
                }
            }
        }
    };
}
