package se.kth.nada.bastianf.eyephone;

import java.util.ArrayList;
import java.util.Locale;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

/**
 * An adapter for an auto-complete text field where a user can input phone
 * numbers and names of recipients. This adapter creates a list with suggestions
 * where the user can pick a recipient by tapping an element in the list instead
 * of entering the whole name or number manually. Each element contains the name
 * and extension of the recipient. The adapter uses an array list of subscriber
 * objects as underlying data source.
 * 
 * You can bind the adapter to an AutoCompleteTextView using the setAdapter
 * method on the AutoCompleteEditText object.
 * 
 * @author Peter Caprioli and Bastian Fredriksson
 * @version
 */
public class RecipientsCompletionsAdapter extends ArrayAdapter<Subscriber> {
	Context context;
	ArrayList<Subscriber> subscribers;
	ArrayList<Subscriber> unvisibleSubscribers;

	public RecipientsCompletionsAdapter(Context context,
			ArrayList<Subscriber> subscribers, ArrayList<Subscriber> filteredData) {
		super(context, R.layout.view_recipient, subscribers);
		this.subscribers = subscribers;
		this.context = context;
		this.unvisibleSubscribers = filteredData;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View recipientView = convertView;
		if (recipientView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			recipientView = inflater.inflate(R.layout.view_recipient, parent,
					false);
		}
		// Get fields
		TextView name = (TextView) recipientView
				.findViewById(R.id.recipient_name);
		TextView number = (TextView) recipientView
				.findViewById(R.id.recipient_number);
		// Set values
		String subscriberName = subscribers.get(position).getName();
		if (subscriberName.equals("")) {
			subscriberName = "N/A";
			name.setTextColor(Color.GRAY);
		}
		name.setText(subscriberName);
		number.setText(String.valueOf(subscribers.get(position).getExtension()));
		return recipientView;
	}

	@Override
	public Filter getFilter() {
		return new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				if (constraint == null) {
					subscribers.addAll(unvisibleSubscribers);
					unvisibleSubscribers.clear();
					return null;
				}
				int skip = 0; // Number of newly added elements to skip
				// Filter using the subscriber's extension and name
				constraint = constraint.toString().toLowerCase(Locale.US);
				for (int i = subscribers.size() - 1; i >= 0; i--) {
					String name = subscribers.get(i).getName();
					String extension = String.valueOf(subscribers.get(i)
							.getExtension());
					if (!name.toLowerCase(Locale.US).startsWith(
							constraint.toString())
							&& !extension.startsWith(constraint.toString())) {
						unvisibleSubscribers.add(subscribers.remove(i));
						skip++;
					}
				}
				for (int i = unvisibleSubscribers.size() - skip - 1; i >= 0; i--) {
					String name = unvisibleSubscribers.get(i).toString();
					String extension = String.valueOf(unvisibleSubscribers.get(
							i).getExtension());
					if (name.toLowerCase(Locale.US).startsWith(
							constraint.toString())
							|| extension.startsWith(constraint.toString())) {
						subscribers.add(unvisibleSubscribers.remove(i));
					}
				}
				// We edit the data source directly, so there is nothing to
				// return
				return null;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				notifyDataSetChanged();
			}
		};
	}
	
	public ArrayList<Subscriber> getFilteredData() {
		return unvisibleSubscribers;
	}

	@Override
	public int getCount() {
		return subscribers.size();
	}
}