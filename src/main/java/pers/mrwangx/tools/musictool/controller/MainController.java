package pers.mrwangx.tools.musictool.controller;

import com.jfoenix.animation.alert.JFXAlertAnimation;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import pers.mrwangx.tool.musictool.config.MusicAPIConfig;
import pers.mrwangx.tool.musictool.entity.Song;
import pers.mrwangx.tools.musictool.App;
import pers.mrwangx.tools.musictool.service.Data;
import pers.mrwangx.tools.musictool.util.FileUtil;
import pers.mrwangx.tools.musictool.util.QQMusicUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * \* Author: MrWangx
 * \* Date: 2019/5/12
 * \* Time: 17:02
 * \* Description:
 **/
public class MainController implements Initializable {

    public static MainController mainController = null;

    private static final Logger LOGGER = Logger.getLogger("MainController");
    private static final String ABOUT_MESSAGE = "此软件供学习和交流用，请支持正版QQ音乐！！！！\n作者:MrWangx";

    private SearchController searchController;
    private MyFavoriteController myFavoriteController;
    private Data<Song> data;

    private Stage stage;
    private JFXAlert<Label> alert;
    private Label alertLabel;
    private MediaPlayer mediaPlayer;
    private Media media;
    private Song crtSong;

    private boolean playing = false;
    private SimpleStringProperty savePath = new SimpleStringProperty(); //存储位置
    private Properties settings;

    private Image pauseImg = new javafx.scene.image.Image("/img/pause.png");
    private Image playImg = new Image("/img/play.png");
    private Image likeImg = new Image("/img/like-green.png");
    private Image likeFillImg = new Image("/img/like-red-fill.png");


    @FXML
    private Pane root;
    @FXML
    private JFXDrawersStack drawersStack;
    @FXML
    private JFXDrawer leftDrawer;
    @FXML
    private VBox choosePane;
    @FXML
    private JFXButton chooseDirBtn;
    @FXML
    private JFXButton downloadDir;
    @FXML
    private JFXButton myFavoriteBtn;
    @FXML
    private JFXToggleButton setCache;
    @FXML
    private JFXButton searchSongBtn;
    @FXML
    private JFXButton aboutBtn;

    //mediatonctrol
    @FXML
    private Label timeLabel;
    @FXML
    private Label crtTimeLabel;
    @FXML
    private Label songinfoLabel;
    @FXML
    private JFXSlider timeSlider;
    @FXML
    private ImageView preSong;
    @FXML
    private ImageView play_pause;
    @FXML
    private ImageView nextSong;
    @FXML
    private ImageView setting;
    @FXML
    private ImageView like;
    @FXML
    private ImageView albumImg;
    @FXML
    private JFXSlider volControl;

    public MainController(Properties settings) {
        setSettings(settings);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainController = this;
        initChoosePane();
        initMediaControls();
        initDrawer();
        initSettings();
    }


    private void initChoosePane() {
        downloadDir.setOnMouseClicked(event -> {
            showDir(downloadDir.getText());
        });
        downloadDir.textProperty().bind(savePath);
        chooseDirBtn.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                DirectoryChooser dirChooser = new DirectoryChooser();
                if (!new File(savePath.get()).exists()) { //文件夹不存在
                    savePath.set(System.getProperty("user.home"));
                }
                dirChooser.setInitialDirectory(new File(savePath.get()));
                File dir = dirChooser.showDialog(stage);
                if (dir != null) {
                    savePath.set(dir.getAbsolutePath());
                }
            }
        });
        myFavoriteBtn.setOnMouseClicked(event -> {
            drawersStack.setContent(myFavoriteController.getRoot());
            leftDrawer.toggle();
        });
        searchSongBtn.setOnMouseClicked(event -> {
            drawersStack.setContent(searchController.getRoot());
            leftDrawer.toggle();
        });
        aboutBtn.setOnMouseClicked(event -> {
            alert(ABOUT_MESSAGE);
        });
    }

    private void initDrawer() {
        leftDrawer.setSidePane(choosePane);
        leftDrawer.setDefaultDrawerSize(290);
        drawersStack.setContent(searchController.getRoot());
        setting.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                drawersStack.toggle(leftDrawer);
            }
        });
    }

    private void initMediaControls() {
        like.setImage(likeImg);
        timeSlider.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY && mediaPlayer != null) pauseMusic();
        });
        timeSlider.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY && mediaPlayer != null) {
                mediaPlayer.seek(new Duration(timeSlider.getValue() / 100.000 * mediaPlayer.getStopTime().toMillis()));
                playMusic();
            }
        });
        timeSlider.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY && mediaPlayer != null)
                mediaPlayer.seek(new Duration(timeSlider.getValue() / 100.000 * mediaPlayer.getStopTime().toMillis()));
        });
        timeSlider.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.RIGHT) {
                mediaPlayer.seek(new Duration(mediaPlayer.getCurrentTime().toMillis() + 5000));
            }
        });

        //上一首
        preSong.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                preSong();
            }
        });
        //暂停/播放
        play_pause.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (playing) {
                    pauseMusic();
                } else {
                    playMusic();
                }
            }
        });
        //下一首
        nextSong.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                nextSong();
            }
        });

        //喜欢
        like.setOnMouseClicked(event -> {
            if (crtSong != null) {
                if (like.getImage() == likeImg) {
                    myFavoriteController.addToMyFavorite(crtSong);
                } else {
                    myFavoriteController.removeFromMyFavorite(crtSong);
                }
            }
        });
    }

    /**
     * 初始化设置
     */
    private void initSettings() {
        if (settings != null) {
            String saveDir = settings.getProperty(App.KEY_SAVEPATH);
            this.savePath.set(saveDir);
            File dir = new File(this.savePath.get());
            if (!dir.exists()) {
                dir.mkdir();
            }

            volControl.setValue(Integer.parseInt(settings.getProperty(App.KEY_VOLUME)));
            setCache.setSelected(Boolean.parseBoolean(settings.getProperty(App.KEY_SETCACHE)));

            setCache.selectedProperty().addListener((observable, oldValue, newValue) -> {
                settings.setProperty(App.KEY_SETCACHE, Boolean.toString(newValue));
            });

            volControl.valueProperty().addListener((observable, oldValue, newValue) -> {
                settings.setProperty(App.KEY_VOLUME, Integer.toString(newValue.intValue()));
            });

            this.savePath.addListener((observable, oldValue, newValue) -> {
                settings.setProperty(App.KEY_SAVEPATH, newValue);
            });

        }
    }

    /**
     * 初始化Alert控件
     */
    public void initAlert() {
        JFXDialogLayout layout = new JFXDialogLayout();
        alertLabel = new Label();
        layout.setBody(alertLabel);
        alert = new JFXAlert<>(stage);
        alert.setOverlayClose(true);
        alert.setAnimation(JFXAlertAnimation.TOP_ANIMATION);
        alert.setContent(layout);
        alert.initModality(Modality.NONE);
    }


    /**
     * 显示提示信息
     *
     * @param message 提示信息
     */
    public void alert(String message) {
        alertLabel.setText(message);
        alert.showAndWait();
    }

    /**
     * 打开文件夹
     *
     * @param filepath
     */
    private void showDir(String filepath) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.OPEN)) {
            try {
                File file = new File(filepath);
                if (file.exists()) {
                    desktop.open(new File(filepath));
                } else {
                    alert("下载文件夹[" + filepath + "]不存在,请重新选择");
                }
            } catch (IOException e) {
                LOGGER.warning("" + e);
                e.printStackTrace();
                alert("打开文件夹错误");
            }
        } else {
            alert("不能打开文件夹");
        }
    }

    /**
     * 播放音乐
     *
     * @param song
     */
    public void playMusic(Song song) {
        if (song != null) {
            Platform.runLater(() -> {
                if (!song.getSongid().matches("^\\s*$")) {
                    Image image = likeImg;
                    for (Song s : myFavoriteController.getSongs()) {
                        if (s.getSongid().equals(song.getSongid())) {
                            image = likeFillImg;
                            break;
                        }
                    }
                    like.setImage(image);
                    LOGGER.info("播放" + song);
                    resetSongInfoDisplay();
                    Task<String> songPlayUrlTask = new Task<String>() {
                        @Override
                        protected String call() throws Exception {
                            return MusicAPIConfig.REAL_SONG_PLAY_URL(song.getMusicType(), song.getSongid());
                        }
                    };
                    songPlayUrlTask.setOnSucceeded(event -> {
                       String resource = song.REAL_SONG_PLAY_URL();
                        LOGGER.info(resource);
                        //缓存是否开启
                        if (setCache.isSelected()) {
                            File cacheFile = FileUtil.getSongCache(song.getSongid());
                            if (cacheFile == null) {
                                File cf = FileUtil.saveSongCache(FileUtil.getRuntimeDir() + App.CACHE_DIR, song.getSongid(), song.REAL_SONG_PLAY_URL());
                                String fUrl = FileUtil.fileToUrlString(cf);
                                resource = cf == null ? resource : fUrl == null ? resource : fUrl;
                            } else {
                                String fUrl = FileUtil.fileToUrlString(cacheFile);
                                resource = fUrl == null ? resource : fUrl;
                            }
                        }
                        LOGGER.info(resource);


                        media = new Media(resource);
                        mediaPlayer = new MediaPlayer(media);

                        int time = song.getDuration();
                        mediaPlayer.setOnReady(() -> {
                            timeLabel.setText(String.format("%02d:%02d", time / 60, time % 60));
                        });

                        mediaPlayer.setOnError(() -> {
                            alert("播放[" + song.getName() + " - " + song.getSinger() + "]失败");
                        });
                        //放歌进度条设置
                        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                            timeSlider.setValue(newValue.toMillis() / mediaPlayer.getStopTime().toMillis() * 100);
                            int t = (int) mediaPlayer.getCurrentTime().toSeconds();
                            crtTimeLabel.setText(String.format("%02d:%02d", t / 60, t % 60));
                        });

                        mediaPlayer.setVolume(volControl.getValue() / 100.00);
                        volControl.valueProperty().addListener((observable, oldValue, newValue) -> {
                            mediaPlayer.setVolume(newValue.doubleValue() / 100.00);
                        });

                        mediaPlayer.setOnEndOfMedia(() -> {
                            LOGGER.info("歌曲放完了");
                            if (crtindex() > -1 && crtindex() < data.getSongs().size() - 1 && !data.getSongs().isEmpty()) {
                                playMusic(data.get(crtindexInc()));
                            }
                        });
                        songinfoLabel.setText(song.getName() + " - " + song.getSinger());
                        crtSong = song;
                        albumImg.setImage(new Image(song.getImgurl()));
                        playMusic();
                    });
                    new Thread(songPlayUrlTask).start();
                }
            });
        }
    }


    /**
     * 上一首
     */
    public void preSong() {
        if (crtindex() > 0 && !data.getSongs().isEmpty()) {
            playMusic(data.get(crtindexDec()));
        }
    }

    /**
     * 暂停音乐
     */
    public void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            play_pause.setImage(playImg);
            playing = false;
        }
    }

    /**
     * 下一首
     */
    public void nextSong() {
        if (crtindex() > -1 && crtindex() < data.getSongs().size() - 1 && !data.getSongs().isEmpty()) {
            playMusic(data.get(crtindexInc()));
        }
    }

    /**
     * 恢复播放
     */
    public void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            play_pause.setImage(pauseImg);
            playing = true;
        }
    }

    /**
     * 重置进度条和时间等信息
     */
    public void resetSongInfoDisplay() {
        timeSlider.setValue(0);
        if (mediaPlayer != null) mediaPlayer.dispose();
        crtTimeLabel.setText("00:00");
        timeLabel.setText("00:00");
    }

    public int crtindex() {
        return data.getCrtindex();
    }

    public int crtindexInc() {
        data.setCrtindex(crtindex() + 1);
        return crtindex();
    }

    public int crtindexDec() {
        data.setCrtindex(crtindex() - 1);
        return crtindex();
    }

    public String getSavePath() {
        return savePath.get();
    }

    public SimpleStringProperty savePathProperty() {
        return savePath;
    }

    public MyFavoriteController getMyFavoriteController() {
        return myFavoriteController;
    }

    public void setMyFavoriteController(MyFavoriteController myFavoriteController) {
        this.myFavoriteController = myFavoriteController;
        myFavoriteController.setMainController(this);
    }

    public void setSearchController(SearchController searchController) {
        this.searchController = searchController;
        searchController.setMainController(this);
    }

    public Pane getRoot() {
        return root;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Data<Song> getData() {
        return data;
    }

    public void setData(Data<Song> data) {
        this.data = data;
    }

    public Properties getSettings() {
        return settings;
    }

    public void setSettings(Properties settings) {
        this.settings = settings;
    }

    public void setToLikeImg() {
        this.like.setImage(likeImg);
    }

    public void setToLikeFillImg() {
        this.like.setImage(likeFillImg);
    }

    public Song getCrtSong() {
        return crtSong;
    }
}
