package myrssaggregator.myrssaggregator;

import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.widget.LinearLayout.*;

public class MyRssList extends AppCompatActivity {

    String linkXml = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_rss_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        List<EditText> etList = new ArrayList<EditText>();

        //mEdit   = (EditText)findViewById(R.id.edittext);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(MyRssList.this).create();
                final EditText inputRss = new EditText(MyRssList.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                inputRss.setLayoutParams(lp);
                alertDialog.setView(inputRss);
                alertDialog.setTitle("New rss flux");
                alertDialog.setMessage("Enter the rss flux link");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
        String link = "https://www.androidpit.com/feed/main.xml";
        if (savedInstanceState == null) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            final Bundle bundle = new Bundle();
            bundle.putString("link", link);
            fragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends ListFragment {

//        private TextView mRssFeed;

        String linkXml = "";

        public PlaceholderFragment() {
        }


//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                                 Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//            mRssFeed = (TextView) rootView.findViewById(R.id.rss_feed);
//            return rootView;
//        }

        @Override
        public void onStart() {
            super.onStart();
            new GetAndroidPitRssFeedTask().execute();
        }

        private String getAndroidPitRssFeed() throws IOException {
            InputStream in = null;
            String rssFeed = null;
            try {
                Bundle args = getArguments();
                if (args  != null && args.containsKey("link"))
                    linkXml = args.getString("link");
                URL url = new URL(linkXml);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                in = conn.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                for (int count; (count = in.read(buffer)) != -1; ) {
                    out.write(buffer, 0, count);
                }
                byte[] response = out.toByteArray();
                rssFeed = new String(response, "UTF-8");
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            return rssFeed;
        }

        private class GetAndroidPitRssFeedTask extends AsyncTask<Void, Void, List<String>> {

            @Override
            protected List<String> doInBackground(Void... voids) {
                List<String> result = null;
                try {
                    String feed = getAndroidPitRssFeed();
                    result = parse(feed);
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return result;
            }

            private List<String> parse(String rssFeed) throws XmlPullParserException, IOException {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(rssFeed));
                xpp.nextTag();
                return readRss(xpp);
            }

            private List<String> readRss(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
                List<String> items = new ArrayList<>();
                parser.require(XmlPullParser.START_TAG, null, "rss");
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String name = parser.getName();
                    if (name.equals("channel")) {
                        items.addAll(readChannel(parser));
                    } else {
                        skip(parser);
                    }
                }
                return items;
            }

            private List<String> readChannel(XmlPullParser parser)
                    throws IOException, XmlPullParserException {
                List<String> items = new ArrayList<>();
                parser.require(XmlPullParser.START_TAG, null, "channel");
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String name = parser.getName();
                    if (name.equals("item")) {
                        items.add(readItem(parser));
                    } else {
                        skip(parser);
                    }
                }
                return items;
            }

            private String readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
                String result = null;
                parser.require(XmlPullParser.START_TAG, null, "item");
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String name = parser.getName();
                    if (name.equals("title")) {
                        result = readTitle(parser);
                    } else {
                        skip(parser);
                    }
                }
                return result;
            }

            // Processes title tags in the feed.
            private String readTitle(XmlPullParser parser)
                    throws IOException, XmlPullParserException {
                parser.require(XmlPullParser.START_TAG, null, "title");
                String title = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, "title");
                return title;
            }

            private String readText(XmlPullParser parser)
                    throws IOException, XmlPullParserException {
                String result = "";
                if (parser.next() == XmlPullParser.TEXT) {
                    result = parser.getText();
                    parser.nextTag();
                }
                return result;
            }

            private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    throw new IllegalStateException();
                }
                int depth = 1;
                while (depth != 0) {
                    switch (parser.next()) {
                        case XmlPullParser.END_TAG:
                            depth--;
                            break;
                        case XmlPullParser.START_TAG:
                            depth++;
                            break;
                    }
                }
            }

            @Override
            protected void onPostExecute(List<String> rssFeed) {
                if (rssFeed != null) {
                    setListAdapter(new ArrayAdapter<>(
                            getActivity(),
                            android.R.layout.simple_list_item_1,
                            android.R.id.text1,
                            rssFeed));
                    /*ListView list  = getListView();
                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1,
                                                int arg2, long arg3) {

                            Toast.makeText(getActivity(), "jkgldfjmlgjkdfjklgjsmlgkdf",
                                    Toast.LENGTH_LONG).show();
                        }

                    });*/
                }
            }
        }
    }
}