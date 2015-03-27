package it.unina.is2project.sensorgames.pong;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;

import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.region.ITextureRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import it.unina.is2project.sensorgames.R;
import it.unina.is2project.sensorgames.database.dao.GiocatoreDAO;
import it.unina.is2project.sensorgames.entity.Giocatore;

import static org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory.createFromResource;

public class GamePongOnePlayer extends GamePong {

    /**
     * Debug
     */
    private final String TAG = "1PlayerGame";

    /*
        Graphics
    */
    // Text View
    private Text txtScore;
    private Text txtEvnt;
    private Text txtLvl;

    // Life
    private BitmapTextureAtlas lifeTexture;
    private ITextureRegion lifeTextureRegion;
    private List<Sprite> lifeSprites = new ArrayList<Sprite>();

    // First enemy
    private Sprite firstEnemy;

    // Rush Hour
    private List<Sprite> rushHour = new ArrayList<Sprite>();
    private List<PhysicsHandler> rushHourHandlers = new ArrayList<PhysicsHandler>();

    // Bonus ball
    private BitmapTextureAtlas bonusBallTexture;
    private ITextureRegion bonusBallTextureRegion;
    private List<Sprite> bonusBalls = new ArrayList<Sprite>();

    // Life bonus
    private Sprite lifeBonus;

    /*
        Game data
    */
    private long score = 0;
    private int gain;
    private static final int MAX_LIFE = 3;
    private int life = MAX_LIFE - 1;
    private int old_life;
    private int hit_count = 0;
    private int reach_count = 5;
    private static final int MIN_REACH_COUNT = 2;
    private static final int MAX_REACH_COUNT = 6;

    // Levels
    private int level;
    private boolean level_one = true;
    private static final int LEVEL_ONE = 0;
    private static final int BARRIER_ONE = 10;
    private boolean level_two = false;
    private static final int LEVEL_TWO = 1;
    private static final int BARRIER_TWO = 300;
    private boolean level_three = false;
    private static final int LEVEL_THREE = 2;
    private static final int BARRIER_THREE = 3000;
    private boolean level_four = false;
    private static final int LEVEL_FOUR = 3;
    private static final int BARRIER_FOUR = 9000;
    private boolean level_five = false;
    private static final int LEVEL_FIVE = 4;
    private static final int BARRIER_FIVE = 18000;
    private boolean level_six = false;
    private static final int LEVEL_SIX = 5;
    private static final int BARRIER_SIX = 36000;
    private boolean level_seven = false;
    private static final int LEVEL_SEVEN = 6;
    private static final int BARRIER_SEVEN = 72000;

    /**
     * Events
     */
    private int game_event;

    // Events' enable
    private boolean new_event = true;
    private boolean first_event = true;
    private boolean no_event = false;
    private boolean rush_hour = false;
    private boolean first_enemy = false;
    private boolean drunk_ball = false;
    private boolean freeze = false;
    private boolean bubble_bonus = false;
    private boolean life_bonus = false;

    // Events' number
    private static final int NO_EVENT = 0;
    private static final int FIRST_ENEMY = 1;
    private static final int BUBBLE_BONUS = 2;
    private static final int DRUNK_BALL = 3;
    private static final int LIFE_BONUS = 4;
    private static final int RUSH_HOUR = 5;
    private static final int FREEZE = 6;

    // Events' data
    private static final int BONUS_BALL_MAX_NUM = 5;
    private static final int BONUS_BALL_MIN_NUM = 3;
    private static int BONUS_BALL_NUM;
    private static final int RUSH_HOUR_MAX_NUM = 30;
    private static final int RUSH_HOUR_MIN_NUM = 15;
    private static int RUSH_HOUR_NUM;
    private boolean life_detached = false;
    private boolean allBonusDetached = false;
    private static float OLD_VEL_X;
    private static float OLD_VEL_Y;

    // Pause utils
    private boolean pause = false;
    private Point directions;
    private float old_x_speed;
    private float old_y_speed;
    private int old_game_speed;
    private long tap;
    private String old_event = "";

    // Game over utils
    private boolean restart_game = false;


    @Override
    protected void loadGraphics() {
        super.loadGraphics();

        /** Life texture loading */
        Drawable starDraw = getResources().getDrawable(R.drawable.life);
        lifeTexture = new BitmapTextureAtlas(getTextureManager(), starDraw.getIntrinsicWidth(), starDraw.getIntrinsicHeight());
        lifeTextureRegion = createFromResource(lifeTexture, this, R.drawable.life, 0, 0);
        lifeTexture.load();

        /** Bonus ball loading */
        Drawable bonusBallDraw = getResources().getDrawable(R.drawable.ball_petrol);
        bonusBallTexture = new BitmapTextureAtlas(getTextureManager(), bonusBallDraw.getIntrinsicWidth(), bonusBallDraw.getIntrinsicHeight());
        bonusBallTextureRegion = createFromResource(bonusBallTexture, this, R.drawable.ball_petrol, 0, 0);
        bonusBallTexture.load();

    }

    @Override
    protected Scene onCreateScene() {
        super.onCreateScene();

        /** Adding the scoring text to the scene */
        txtScore = new Text(10, 10, font, "", 20, getVertexBufferObjectManager());
        scene.attachChild(txtScore);

        /** Adding the level text to the scene */
        txtLvl = new Text(10, txtScore.getY() + txtScore.getHeight(), font, "", 20, getVertexBufferObjectManager());
        scene.attachChild(txtLvl);

        /** Adding the level text to the scene */
        txtEvnt = new Text(10, txtLvl.getY() + txtLvl.getHeight(), font, "", 20, getVertexBufferObjectManager());
        scene.attachChild(txtEvnt);

        /** Adding the life sprites to the scene */
        addLifeSpritesToScene();

        txtScore.setText(getResources().getString(R.string.text_score) + ": " + score);

        /** Setting up the physics of the game */
        settingPhysics();

        return scene;
    }

    private void addLifeSpritesToScene(){
        for ( int i = 1 ; i <= life+1 ; i++ ){
            Sprite lifeSprite = new Sprite(0, 0, lifeTextureRegion,getVertexBufferObjectManager());
            lifeSprite.setX(CAMERA_WIDTH - i*lifeSprite.getWidth());
            lifeSprites.add(lifeSprite);
            scene.attachChild(lifeSprites.get(i-1));
        }
    }

    @Override
    protected void setBallVeloctity() {
        super.setBallVeloctity();
        GAME_VELOCITY = 2;
    }

    @Override
    protected void clearGame() {
        super.clearGame();
        clearEvents();
        life = MAX_LIFE - 1;
        pause = false;
        game_event = NO_EVENT;
        score = 0;
        addLifeSpritesToScene();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        clearGame();
        ballSprite.setPosition((CAMERA_WIDTH - ballSprite.getWidth())/2, (CAMERA_HEIGHT - ballSprite.getHeight())/2);
        handler.setVelocity(BALL_SPEED, -BALL_SPEED);
        scene.attachChild(ballSprite);
    }

    @Override
    protected void collidesBottom() {
        Log.d(TAG, "Bottom. V(X,Y): " + handler.getVelocityX() + "," + handler.getVelocityY());
        previous_event = BOTTOM;

        /**  The ballSprite is detached */
        ballSprite.detachSelf();
        /** The lifeSprite is detached */
        lifeSprites.get(life).detachSelf();
        /** Life count is decremented if not drunk ball event */
        life--;

        if(game_event == DRUNK_BALL) {
            Log.i(TAG,"Drunk ball stopped");
            new_event = true;
            callEvent();
        }

        /** If the life count is less equal than 0, the game is over */
        if (life < 0) {
            gameOver();
        }
        /** Else replace the ball */
        else {
            /** Setting the position on centre of screen */
            ballSprite.setPosition((CAMERA_WIDTH - ballSprite.getWidth()) / 2, (CAMERA_HEIGHT - ballSprite.getHeight()) / 2);
            /** Set the direction upward */
            handler.setVelocityY(-handler.getVelocityY());
            /** The ballSprite is attached */
            scene.attachChild(ballSprite);
        }

    }

    @Override
    protected void collidesBar() {
        super.collidesBar();
        addScore();
        hit_count++;
        Log.i(TAG,"Hit count " + hit_count);
        if(hit_count == reach_count) {
            new_event = true;
            callEvent();
        }
    }

    @Override
    protected void gameLevels(){
        /** This procedure understand what modifier needs according to the score */
        if(score < BARRIER_ONE && level_one){
            level = LEVEL_ONE;
            txtLvl.setText(getApplicationContext().getString(R.string.text_lv1));
        }

        if(score >= BARRIER_ONE && score < BARRIER_TWO && !level_two) {
            level = LEVEL_TWO;
            GAME_VELOCITY *= 2;
            level_two = true;
            txtLvl.setText(getApplicationContext().getString(R.string.text_lv2));
        }

        if(score >= BARRIER_TWO && score < BARRIER_THREE && !level_three) {
            level = LEVEL_THREE;
            handler.setVelocity(handler.getVelocityX() * 2, handler.getVelocityY() * 2);
            level_three = true;
            txtLvl.setText(getApplicationContext().getString(R.string.text_lv3));
        }

        if(score >= BARRIER_THREE && score < BARRIER_FOUR && !level_four) {
            level = LEVEL_FOUR;
            barSprite.setWidth(0.2f * CAMERA_WIDTH);
            level_four = true;
            txtLvl.setText(getApplicationContext().getString(R.string.text_lv4));
        }

        if(score >= BARRIER_FOUR && score < BARRIER_FIVE && !level_five) {
            level = LEVEL_FIVE;
            handler.setVelocity(handler.getVelocityX() * 1.5f, handler.getVelocityY() * 1.5f);
            barSprite.setWidth(0.3f * CAMERA_WIDTH);
            level_five = true;
            txtLvl.setText(getApplicationContext().getString(R.string.text_lv5));
        }

        if(score >= BARRIER_FIVE && score < BARRIER_SIX && !level_six) {
            level = LEVEL_SIX;
            barSprite.setWidth(0.2f * CAMERA_WIDTH);
            level_six = true;
            txtLvl.setText(getApplicationContext().getString(R.string.text_lv6));
        }

        if(score >= BARRIER_SEVEN && !level_seven) {
            level = LEVEL_SEVEN;
            handler.setVelocity(handler.getVelocityX() * 1.5f, handler.getVelocityY() * 1.5f);
            level_seven = true;
            txtLvl.setText(getApplicationContext().getString(R.string.text_lv7));
        }
    }

    @Override
    protected void gameEvents(){

        /** Handling game events collisions */
        gameEventsCollisionLogic();

        /** Handling events logic */
        switch(game_event){
            case NO_EVENT:
                if(!no_event) {
                    txtEvnt.setText("");
                    no_event = true;
                }
                break;

            case RUSH_HOUR:
                if(!rush_hour){
                    txtEvnt.setText(getApplicationContext().getString(R.string.text_rush));
                    rush_hour = true;
                    rushHourLogic();
                }
                break;

            case DRUNK_BALL:
                if(!drunk_ball){
                    txtEvnt.setText(getApplicationContext().getString(R.string.text_drunkball));
                    drunk_ball = true;
                }
                drunkBallLogic(); // Called every update

                break;

            case FIRST_ENEMY:
                if(!first_enemy){
                    txtEvnt.setText(getApplicationContext().getString(R.string.text_first_enemy));
                    first_enemy = true;
                    firstEnemyLogic();
                }
                break;

            case BUBBLE_BONUS:
                if(!bubble_bonus){
                    txtEvnt.setText(getApplicationContext().getString(R.string.text_bubble));
                    bubble_bonus = true;
                    bubbleBonusLogic();
                }
                break;

            case FREEZE:
                if(!freeze){
                    txtEvnt.setText(getApplicationContext().getString(R.string.text_freeze));
                    freeze = true;
                    freezeLogic();
                }
                break;

            case LIFE_BONUS:
                if(!life_bonus){
                    txtEvnt.setText(getApplicationContext().getString(R.string.text_lifebonus));
                    life_bonus = true;
                    lifeBonusLogic();
                }
                break;
        }

        /** Set score section */
        txtScore.setText(getResources().getString(R.string.text_score) + ": " + score);

        /** Handling game restarting */
        if(restart_game) {
            Log.i(TAG,"Game restarted");
            onRestart();
            restart_game = false;
        }

    }

    @Override
    protected void gameOver(){
        game_over = true;
        handler.setVelocity(0);
        GAME_VELOCITY = 0;
        touch.stop();
        txtEvnt.setText(getApplicationContext().getString(R.string.text_gameover));

        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                /** Game over dialog */
                AlertDialog.Builder alert = new AlertDialog.Builder(GamePongOnePlayer.this);

                alert.setTitle(getApplicationContext().getResources().getString(R.string.text_ttl_oneplayer_savegame));
                alert.setMessage(getApplicationContext().getResources().getString(R.string.text_msg_oneplayer_savegame));

                // Set an EditText view to get user input
                final EditText input = new EditText(GamePongOnePlayer.this);
                alert.setView(input);

                alert.setPositiveButton(getApplicationContext().getResources().getString(R.string.text_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();

                        //TODO idGiocatore sarà un campo della classe e la variabile locale verrà rimossa
                        int idGiocatore = 1;
                        // TODO: Salvataggio in DB
                        GiocatoreDAO giocatoreDAO = new GiocatoreDAO(getApplicationContext());
                        Giocatore g = giocatoreDAO.findById(idGiocatore);
                        if (g == null){
                            g = new Giocatore("Francesco",0,0,0);
                            giocatoreDAO.insert(g);
                        }
                        else {
                            g.setPartiteGiocateSingolo(g.getPartiteGiocateSingolo() + 1);
                            giocatoreDAO.update(g);
                        }
                        Log.i(TAG,"Partite giocate: "+g.getPartiteGiocateSingolo());

                        restart_game = true;
                        game_over = false;
                    }
                });

                alert.setNegativeButton(getApplicationContext().getResources().getString(R.string.text_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        restart_game = true;
                        game_over = false;
                    }
                });

                alert.show();
            }
        });
    }

    @Override
    public void addScore() {
        /** This procedure increase the score according to the current score. */
        if(score >= 0 && score < BARRIER_ONE) {
            score += 2;
            gain = 2;
        }

        if(score >= BARRIER_ONE && score < BARRIER_TWO) {
            score += 10;
            gain = 10;
        }

        if(score >= BARRIER_TWO && score < BARRIER_THREE) {
            score += 20;
            gain = 20;
        }

        if(score >= BARRIER_THREE && score < BARRIER_FOUR) {
            score += 30;
            gain = 30;
        }

        if(score >= BARRIER_FOUR && score < BARRIER_FIVE) {
            score += 60;
            gain = 60;
        }

        if(score >= BARRIER_FIVE && score < BARRIER_SIX) {
            score += 90;
            gain = 90;
        }

        if(score >= BARRIER_SEVEN) {
            score += 150;
            gain = 150;
        }

        if(game_event == DRUNK_BALL)
            score += 10000;

        if(game_event == RUSH_HOUR)
            score += gain*4;

        if(game_event == FIRST_ENEMY)
            score += gain*3;

        Log.i("addScore()","Score: " + score);
    }

    @Override
    public void remScore() {
        // do nothing
    }

    @Override
    public void actionDownEvent() {
        if(!pause) {
            pauseGame();
        }

        if(pause && (System.currentTimeMillis() - tap > 500)){
            restartGame();
        }
    }

    @Override
    public void onBackPressed() {
        if(!pause)
            pauseGame();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.text_msg_oneplayer_dialog)).setTitle(getResources().getString(R.string.text_msg_oneplayer_leavegame)).setPositiveButton(getResources().getString(R.string.text_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked YES button
                finish();
            }
        }).setNegativeButton(getResources().getString(R.string.text_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                restartGame();
            }
        }).show();

        AlertDialog dialog = builder.create();
    }

    private void pauseGame(){
        old_event = (String) txtEvnt.getText();
        txtEvnt.setText(getResources().getString(R.string.text_pause));
        directions = getDirections();
        old_x_speed = handler.getVelocityX();
        old_y_speed = handler.getVelocityY();
        old_game_speed = GAME_VELOCITY;
        tap = System.currentTimeMillis();
        handler.setVelocity(0);
        GAME_VELOCITY = 0;
        pause = true;
    }

    private void restartGame(){
        txtEvnt.setText(old_event);
        handler.setVelocity(old_x_speed, old_y_speed);
        GAME_VELOCITY = old_game_speed;
        pause = false;
    }


    private void rushHourLogic(){
        Random random = new Random();

        RUSH_HOUR_NUM = RUSH_HOUR_MIN_NUM + random.nextInt(RUSH_HOUR_MAX_NUM - RUSH_HOUR_MIN_NUM);

        for(int i = 0 ; i < RUSH_HOUR_NUM ; i++){
            Sprite rush = new Sprite(0, 0, ballTextureRegion, getVertexBufferObjectManager());
            rush.setPosition(random.nextInt(CAMERA_WIDTH), random.nextInt(CAMERA_HEIGHT) - ballSprite.getHeight()*2);
            rush.setWidth(CAMERA_WIDTH * 0.1f);
            rush.setHeight(CAMERA_WIDTH * 0.1f);
            rushHour.add(rush);

            PhysicsHandler physicsHandler = new PhysicsHandler(rushHour.get(i));
            physicsHandler.setVelocity(BALL_SPEED * (random.nextFloat() - random.nextFloat()), BALL_SPEED * (random.nextFloat() - random.nextFloat()) );
            rushHourHandlers.add(physicsHandler);

            rushHour.get(i).registerUpdateHandler(rushHourHandlers.get(i));

            scene.attachChild(rushHour.get(i));
        }
    }

    private void clearRushHour(){
        do {
            rushHour.get(0).detachSelf();
            rushHour.remove(0);
            rushHourHandlers.remove(0);
        } while ( rushHour.size() > 0 );

        rush_hour = false;
    }

    private void drunkBallLogic(){
        if(ballSprite.getY() < CAMERA_HEIGHT/2){
            if(ballSprite.getX() > ballSprite.getWidth() || ballSprite.getX() < CAMERA_WIDTH - ballSprite.getWidth())
                handler.setVelocityX(-handler.getVelocityX());
            else {
                Random random = new Random();
                handler.setVelocityX(random.nextFloat() * BALL_SPEED);
            }
        }
    }

    private void clearDrunkBall(){
        if(level > LEVEL_ONE && level <= LEVEL_TWO)
            handler.setVelocity(BALL_SPEED, -BALL_SPEED);

        if(level >= LEVEL_THREE && level <= LEVEL_FOUR)
            handler.setVelocity(BALL_SPEED * 2, -BALL_SPEED * 2);

        if(level >= LEVEL_FIVE && level <= LEVEL_SIX)
            handler.setVelocity(BALL_SPEED * 1.5f * 2, BALL_SPEED * 1.5f * 2);

        if(level >= LEVEL_SEVEN)
            handler.setVelocity(BALL_SPEED * 1.5f * 1.5f * 2, BALL_SPEED * 1.5f * 1.5f * 2);

        drunk_ball = false;
    }

    private void firstEnemyLogic(){
        firstEnemy = new Sprite(0, CAMERA_HEIGHT/3, barTextureRegion, getVertexBufferObjectManager());
        firstEnemy.setWidth(CAMERA_WIDTH);
        scene.attachChild(firstEnemy);
    }

    private void clearFirstEnemy(){
        first_enemy = false;
        firstEnemy.detachSelf();
    }

    private void bubbleBonusLogic(){
        Random random = new Random();

        BONUS_BALL_NUM = BONUS_BALL_MIN_NUM + random.nextInt(BONUS_BALL_MAX_NUM - BONUS_BALL_MIN_NUM) + 1;

        /** Adding the bonus ball sprites to the scene */
        for ( int i = 0 ; i < BONUS_BALL_NUM ; i++ ){
            random = new Random();
            Sprite bonusSprite = new Sprite(0, 0, bonusBallTextureRegion, getVertexBufferObjectManager());
            int ballRadius = (int)bonusSprite.getHeight()/2;
            int bonusSpriteX = ballRadius + random.nextInt(CAMERA_WIDTH - ballRadius*2);
            bonusSprite.setPosition(bonusSpriteX, (bonusSprite.getHeight()/2)*(i+1));
            bonusSprite.setWidth(CAMERA_WIDTH * 0.1f);
            bonusSprite.setHeight(CAMERA_WIDTH * 0.1f);
            bonusBalls.add(bonusSprite);
            scene.attachChild(bonusBalls.get(i));
        }
        Log.i(TAG , "BONUS_BALL_NUM: " + BONUS_BALL_NUM + " bonusBalls.size(): " + bonusBalls.size());

    }

    private void clearBubbleBonus(){
        if(!allBonusDetached) {
            Log.i(TAG,"Not all bonus ball detached");
            do {
                bonusBalls.get(0).detachSelf();
                bonusBalls.remove(0);
                Log.i(TAG,"Bonus ball detached in clear");
            } while (bonusBalls.size() > 0);
        }
        bubble_bonus = false;
        allBonusDetached = false;
}

    private void lifeBonusLogic(){
        old_life = life;
        if(life < MAX_LIFE - 1) {
            Random random = new Random();
            lifeBonus = new Sprite(0, 0, lifeTextureRegion, getVertexBufferObjectManager());
            lifeBonus.setWidth(CAMERA_WIDTH * 0.1f);
            lifeBonus.setHeight(CAMERA_WIDTH * 0.1f);
            lifeBonus.setPosition(random.nextInt(CAMERA_WIDTH - (int)lifeBonus.getWidth()), random.nextInt(CAMERA_HEIGHT - 2*(int)ballSprite.getHeight()));
            scene.attachChild(lifeBonus);
        }
    }

    private void clearLifeBonus(){
        if(!life_detached && life < MAX_LIFE - 1)
            lifeBonus.detachSelf();
        life_bonus = false;
        life_detached = false;
    }

    private void freezeLogic(){
        handler.setVelocity(handler.getVelocityX() / 2, handler.getVelocityY() / 2);
    }

    private void clearFreeze(){
        handler.setVelocity(handler.getVelocityX()*2, handler.getVelocityY()*2);
        freeze = false;
    }

    private void gameEventsCollisionLogic(){
        switch (game_event){
            case FIRST_ENEMY:
                firstEnemyCollisions();
                break;

            case BUBBLE_BONUS:
                bubbleBonusCollisions();
                break;

            case RUSH_HOUR:
                rushHourCollisions();
                break;

            case LIFE_BONUS:
                lifeBonusCollisions();
                break;
        }

    }

    private void firstEnemyCollisions(){
        if(ballSprite.collidesWith(firstEnemy) && first_enemy && ballSprite.getY() < CAMERA_HEIGHT/2){
            handler.setVelocityY(-handler.getVelocityY());
            touch.play();
        }
    }

    private void bubbleBonusCollisions(){
        for ( int i = 0 ; i < bonusBalls.size() ; i++ ){
            if(ballSprite.collidesWith(bonusBalls.get(i))){
                Log.i(TAG,"Bonus Ball " + i + " removed");
                bonusBalls.get(i).detachSelf();
                bonusBalls.remove(i);
                score += 20 * (level+1);
                if(bonusBalls.size()==0) {
                    allBonusDetached = true;
                    Log.i(TAG, "All bonus ball detached by player");
                }
            }
        }
    }

    private void rushHourCollisions(){
        int rL = CAMERA_WIDTH - (int) ballSprite.getWidth() / 2;
        int bL = CAMERA_HEIGHT - (int) ballSprite.getHeight() / 2;

        for(int i = 0 ; i < rushHour.size() ; i++){
            if ((rushHour.get(i).getX() > rL - (int) ballSprite.getWidth() / 2)) {
                rushHourHandlers.get(i).setVelocityX(-rushHourHandlers.get(i).getVelocityX());
            }
            if (rushHour.get(i).getX() < 0) {
                rushHourHandlers.get(i).setVelocityX(-rushHourHandlers.get(i).getVelocityX());
            }
            if (rushHour.get(i).getY() < 0) {
                rushHourHandlers.get(i).setVelocityY(-rushHourHandlers.get(i).getVelocityY());
            }
            if ((rushHour.get(i).getY() > bL - (int) ballSprite.getHeight() / 2)) {
                rushHourHandlers.get(i).setVelocityY(-rushHourHandlers.get(i).getVelocityY());
            }
        }
    }

    private void lifeBonusCollisions(){
        if(ballSprite.collidesWith(lifeBonus) && life < MAX_LIFE - 1 && old_life == life){
            lifeBonus.detachSelf();
            life++;
            scene.attachChild(lifeSprites.get(life));
            life_detached = true;
            Log.i(TAG,"Star collision. Life: " + life);
        }
    }


    private void clearEvents() {
        switch (game_event) {
            case NO_EVENT:
                no_event = false;
                break;

            case DRUNK_BALL:
                clearDrunkBall();
                break;

            case FIRST_ENEMY:
                clearFirstEnemy();
                break;

            case BUBBLE_BONUS:
                clearBubbleBonus();
                break;

            case RUSH_HOUR:
                clearRushHour();
                break;

            case FREEZE:
                clearFreeze();
                break;

            case LIFE_BONUS:
                clearLifeBonus();
                break;
        }
    }

    private void callEvent() {
        if(!first_event)
            clearEvents();
        else first_event = false;

        if (new_event) {
            Random random = new Random();

            int randomInt = random.nextInt(level+1);

            if (level > LEVEL_ONE) {
                if (game_event == randomInt) {
                    game_event++;
                    if (game_event > level)
                        game_event = 0;
                } else game_event = randomInt;

                if(game_event == LIFE_BONUS && life == MAX_LIFE-1)
                    game_event++;
            }

            reach_count = MIN_REACH_COUNT + random.nextInt(MAX_REACH_COUNT - MIN_REACH_COUNT) + 1;
            Log.i(TAG, "Reach count " + reach_count);
            hit_count = 0;

            new_event = false;
        }
    }

    @Override
    protected void bluetoothExtra() {
        // do nothing
    }
}
