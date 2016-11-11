package homestudio.com.listreload;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;
import homestudio.com.listreload.PullToRefreshListView.OnRefreshListener;
import homestudio.com.listreload.PullToRefreshListViewSampleActivity.PullToRefreshListViewSampleAdapter.ViewHolder;

import java.util.ArrayList;

public class PullToRefreshListViewSampleActivity extends Activity {

    private PullToRefreshListView listView;
    private PullToRefreshListViewSampleAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        listView = (PullToRefreshListView) findViewById(R.id.pull_to_refresh_listview);

        listView.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {
                adapter.loadData();
                listView.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        listView.onRefreshComplete();
                    }
                }, 5000);
            }
        });

        adapter = new PullToRefreshListViewSampleAdapter() {};
        listView.setAdapter(adapter);

        adapter.loadData();

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                ViewHolder viewHolder = (ViewHolder) arg1.getTag();
                if (viewHolder.name != null){
                    Toast.makeText(PullToRefreshListViewSampleActivity.this, viewHolder.name.getText(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public abstract class PullToRefreshListViewSampleAdapter extends android.widget.BaseAdapter {

        private ArrayList<String> items = new ArrayList<>();

        public class ViewHolder {
            public String id;
            public TextView name;
        }

        public void loadData() {
            items = new ArrayList<>();

            items.add("Нью-Йорк");
            items.add("Неаполь");
            items.add("Лондон");
            items.add("Мадрид");
            items.add("Люксембург");
            items.add("Люблин");
            items.add("Маракай");
            items.add("Найроби");
            items.add("Остин");
            items.add("Палермо");
            items.add("Сонгеа");
            items.add("Томск");
            items.add("Одесса");
            items.add("Ниоро");
            items.add("Нанюки");

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            String record = (String) getItem(position);
            LayoutInflater inflater = PullToRefreshListViewSampleActivity.this.getLayoutInflater();
            ViewHolder viewHolder = new ViewHolder();

            if (convertView == null){
                rowView = inflater.inflate(R.layout.list_item,null);
                viewHolder.name = (TextView) rowView.findViewById(R.id.textView1);
                rowView.setTag(viewHolder);
            }

            final ViewHolder holder = (ViewHolder) rowView.getTag();
            holder.name.setText(record);
            return rowView;
        }
    }
}