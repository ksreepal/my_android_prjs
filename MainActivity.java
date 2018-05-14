package chat.vl.com.testchatapp;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import android.widget.Button;
import android.widget.EditText;
import android.text.TextUtils;
import android.widget.ImageView;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import android.widget.Toast;
import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.speech.tts.TextToSpeech;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.content.ActivityNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AIListener {

    private final int REQ_CODE_SPEECH_INPUT = 100;
    TextToSpeech textToSpeech;
    Boolean flagFab = true;
    RelativeLayout addBtn;
    EditText msgInputText;
    String msgContent;
    private AIService aiService;
    int newMsgPosition;
    ChatAppMsgAdapter chatAppMsgAdapter;
    AIRequest aiRequest;
    AIDataService aiDataService;

    //List<ChatAppMsgDTO> msgDtoList;
    List<ChatAppMsgDTO> msgDtoList = new ArrayList<ChatAppMsgDTO>();
    RecyclerView msgRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {

            makeRequest();
        }

        msgInputText = (EditText)findViewById(R.id.chat_input_msg);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},1);
        addBtn = (RelativeLayout)findViewById(R.id.addBtn);

        msgRecyclerView = (RecyclerView)findViewById(R.id.chat_recycler_view);
        msgRecyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        msgRecyclerView.setLayoutManager(linearLayoutManager);

        final AIConfiguration config = new AIConfiguration("f4220e6d0dfe417da9ff28a6135f6a6e",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        aiDataService = new AIDataService(config);
        aiRequest = new AIRequest();

        //msgDtoList = new ArrayList<ChatAppMsgDTO>();
        //ChatAppMsgDTO msgDto = new ChatAppMsgDTO(ChatAppMsgDTO.MSG_TYPE_RECEIVED, "hello");
        //msgDtoList.add(msgDto);

        //ChatAppMsgAdapter chatAppMsgAdapter = new ChatAppMsgAdapter(msgDtoList);
        chatAppMsgAdapter = new ChatAppMsgAdapter(msgDtoList);

//msgRecyclerView.setAdapter(chatAppMsgAdapter);

        addBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                msgContent = msgInputText.getText().toString().trim();

                if (!msgContent.equals("")) {
                    // Add a new sent message to the list.
                    ChatAppMsgDTO msgDto = new ChatAppMsgDTO(ChatAppMsgDTO.MSG_TYPE_SENT, msgContent);
                    msgDtoList.add(msgDto);
                    chatAppMsgAdapter.notifyDataSetChanged();

                    int newMsgPosition = msgDtoList.size() - 1;
                    // Notify recycler view insert one new data.
                    chatAppMsgAdapter.notifyItemInserted(newMsgPosition);
                    // Scroll RecyclerView to the last message.
                    msgRecyclerView.scrollToPosition(newMsgPosition);

                    aiRequest.setQuery(msgContent);

                        new AsyncTask<AIRequest,Void,AIResponse>(){

                            @Override
                            protected AIResponse doInBackground(AIRequest... aiRequests) {
                                final AIRequest request = aiRequests[0];
                                try {
                                    final AIResponse response = aiDataService.request(aiRequest);
                                    return response;
                                } catch (AIServiceException e)
                                    {
                                    }
                                return null;
                            }
                            @Override
                            protected void onPostExecute(AIResponse response) {
                                if (response != null) {

                                    Result result = response.getResult();
                                    String reply = result.getFulfillment().getSpeech();

                                    ChatAppMsgDTO msgDto = new ChatAppMsgDTO(ChatAppMsgDTO.MSG_TYPE_RECEIVED, reply);
                                    msgDtoList.add(msgDto);
                                    chatAppMsgAdapter.notifyDataSetChanged();
                                    int newMsgPosition = msgDtoList.size() - 1;
                                    // Notify recycler view insert one new data.
                                    chatAppMsgAdapter.notifyItemInserted(newMsgPosition);
                                    // Scroll RecyclerView to the last message.
                                    msgRecyclerView.scrollToPosition(newMsgPosition);
                                }
                            }
                        }.execute(aiRequest);
                }
                else
                {
                   promptSpeechInput();

                    //if(strText != null) {
                    // Add a new sent message to the list.
                   // ChatAppMsgDTO msgDto = new ChatAppMsgDTO(ChatAppMsgDTO.MSG_TYPE_SENT, text);
                   // msgDtoList.add(msgDto);

                    //chatAppMsgAdapter.notifyDataSetChanged();

                    /*if(msgDtoList.size() != 0)
                        newMsgPosition = msgDtoList.size()-1;
                    else
                        newMsgPosition = msgDtoList.size();*/

                    // Notify recycler view insert one new data.
                    //chatAppMsgAdapter.notifyItemInserted(newMsgPosition);
                    //chatAppMsgAdapter.notifyDataSetChanged();

                    // Scroll RecyclerView to the last message.
                    //msgRecyclerView.scrollToPosition(newMsgPosition);

                    //ai service listening
                    //aiService.startListening();
                }
                // Empty the input edit text box.
                msgInputText.setText("");
            }
        });

        //edit text code
        msgInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = (ImageView)findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(),R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(),R.drawable.ic_mic_white_24dp);


                if (s.toString().trim().length()!=0 && flagFab){
                    ImageViewAnimatedChange(MainActivity.this,fab_img,img);
                    flagFab=false;
                }
                else if (s.toString().trim().length()==0){
                    ImageViewAnimatedChange(MainActivity.this,fab_img,img1);
                    flagFab=true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        msgRecyclerView.setAdapter(chatAppMsgAdapter);

    }

    private void promptSpeechInput()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        }
        catch (ActivityNotFoundException a)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK)
        {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String speechtext = result.get(0).toString();

    int newMsgPosition = msgDtoList.size() - 1;
    // Notify recycler view insert one new data.
    chatAppMsgAdapter.notifyItemInserted(newMsgPosition);
    // Scroll RecyclerView to the last message.
    msgRecyclerView.scrollToPosition(newMsgPosition);

            // Add a new sent message to the list.
            ChatAppMsgDTO msgDto = new ChatAppMsgDTO(ChatAppMsgDTO.MSG_TYPE_SENT, speechtext);
            msgDtoList.add(msgDto);
            chatAppMsgAdapter.notifyDataSetChanged();

            aiRequest.setQuery(speechtext);

                    new AsyncTask<AIRequest,Void,AIResponse>(){

                        @Override
                        protected AIResponse doInBackground(AIRequest... aiRequests) {
                            final AIRequest request = aiRequests[0];
                            try {
                                final AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e)
                            {
                            }
                            return null;
                        }
                        @Override
                        protected void onPostExecute(AIResponse response) {
                            if (response != null) {

                                Result result = response.getResult();
                                String reply = result.getFulfillment().getSpeech();

                               // if (reply.length()!=0)
                               // {
                                ChatAppMsgDTO msgDto = new ChatAppMsgDTO(ChatAppMsgDTO.MSG_TYPE_RECEIVED, reply);
                                msgDtoList.add(msgDto);
                                chatAppMsgAdapter.notifyDataSetChanged();
                                int newMsgPosition = msgDtoList.size() - 1;
                                // Notify recycler view insert one new data.
                                chatAppMsgAdapter.notifyItemInserted(newMsgPosition);
                                // Scroll RecyclerView to the last message.
                                msgRecyclerView.scrollToPosition(newMsgPosition);
                                //speech out
                                textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, null);
                              //  }

                            }
                        }
                    }.execute(aiRequest);

            //ai service listening
            aiService.startListening();
        }
        //super.onActivityResult(requestCode, resultCode, data);
    }



    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                101);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 101: {
                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                } else {

                }
                return;
            }
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

    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onResult(AIResponse response) {

        //Toast.makeText(getApplicationContext(), " vl speeking...",Toast.LENGTH_SHORT).show();

        //textToSpeech.speak("ValueLabs", TextToSpeech.QUEUE_FLUSH, null);

        Result result = response.getResult();
        String message = result.getResolvedQuery();
        //todo

        chatAppMsgAdapter.notifyDataSetChanged();

        String reply = result.getFulfillment().getSpeech();


        //ChatAppMsgDTO msgDto = new ChatAppMsgDTO(ChatAppMsgDTO.MSG_TYPE_RECEIVED, reply);
        //msgDtoList.add(msgDto);

        //textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, null);

        chatAppMsgAdapter.notifyDataSetChanged();
    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}