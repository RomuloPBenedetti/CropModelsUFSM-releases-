package cropModelsUFSM.control;

import cropModelsUFSM.data.Tuple;
import cropModelsUFSM.data.task.SerializableSimulation;
import cropModelsUFSM.data.task.SimulationInput;
import cropModelsUFSM.data.task.VisualizableSimulation;
import cropModelsUFSM.graphic.AnimationEvents;
import cropModelsUFSM.graphic.ImageAnimation;
import cropModelsUFSM.support.Util;
import cropModelsUFSM.task.concreteTask.UnserializeSimulationTask;
import cropModelsUFSM.task.Task;
import cropModelsUFSM.task.taskInterfaces.TaskObserver;
import cropModelsUFSM.task.abstractTasks.MeteorologicFileTask;
import cropModelsUFSM.task.abstractTasks.SimulationTask;
import cropModelsUFSM.task.abstractTasks.VisualizationTask;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static cropModelsUFSM.support.Util.*;

/**
 * @author romulo Pulcinelli Benedetti
 * @see cropModelsUFSM
 */
public abstract class GuiController
        extends GenericFXController implements TaskObserver {

    /**
     *
     */
    @FXML private ImageView upImv, downImv;
    @FXML private MediaView video;

    @FXML private ImageView okImv, playImv;

    @FXML private AnchorPane simulation, tables, charts;
    @FXML private AnchorPane meteorologic, development, mass;
    @FXML private VBox meteorologicLegend, developmentLegend, massLegend;

    @FXML public Label dataLabel;
    @FXML private Shape dataShape;

    @FXML private ComboBox<String> simulationMenuA, simulationMenuB;
    @FXML private ComboBox<String> simulationYearA, simulationYearB;

    @FXML public AnchorPane warningPane;
    @FXML private Circle warningButton, simulationButtonCircle;

    /**
     *
     */
    private static Tooltip tip = new Tooltip("");
    public static List<SerializableSimulation> currentSimulation;
    private Rectangle2D galeryViewport;
    private Stage helpStage, aboutStage, legendStage, configStage;
    private static Stage warningStage;

/*******************************************************************************

                            Class Initialization

*******************************************************************************/

    /**
     *
     * @param stage
     */
    @Override
    public void postInitializeTasks (Stage stage)
    {
        super.postInitializeTasks(stage);
        menusBindInitialization();
        InteractionElementsInitialization();
        decideAnimation();
        valueListeners();
        coldStart();
    }

    /**
     *
     */
    public void menusBindInitialization()
    {

        simulationMenuA.selectionModelProperty().bind(simulationMenuB.selectionModelProperty());
        simulationMenuA.valueProperty().bind(simulationMenuB.valueProperty());
        simulationMenuA.itemsProperty().bind(simulationMenuB.itemsProperty());

        simulationYearA.selectionModelProperty().bind(simulationYearB.selectionModelProperty());
        simulationYearA.valueProperty().bind(simulationYearB.valueProperty());
        simulationYearA.itemsProperty().bind(simulationYearB.itemsProperty());

    }

    /**
     *
     */
    protected abstract void InteractionElementsInitialization();

    /**
     *
     */
    public void decideAnimation ()
    {
        try {
            String source = loader.getResource("vd.mp4").toURI().toURL().toString();
            Media media = new Media(source);
            System.out.println(source);
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            video.setMediaPlayer(mediaPlayer);
            mediaPlayer.play();
        } catch(Exception ex) {
            // sistemas que não tem suporte correto ao libavformat/codec vão gerar exception
            ex.printStackTrace();
        }
        finally {
            if(video.getMediaPlayer() == null) {
                ImageAnimation imageAnimation = new ImageAnimation(upImv,downImv);
                imageAnimation.play();
            }
        }
    }


    /**
     * This method detect on program start up, if there is any old results and/or meteorologic
     * data stored on the program root directory, presenting data range for any meteorologic
     * file found and showing the results list in SimulationMenus if any old result is found.
     */
    public void coldStart() {
        File resultFolder = new File(resultFolderpath);
        File meteorologicF = new File(meteorologicFile);

        if(meteorologicF.exists()) {
            MeteorologicFileTask meteorologicTask;
            meteorologicTask = newMeteorologicFileTask(meteorologicF, this);
            meteorologicTask.execute();
        }

        if( resultFolder.isDirectory() && resultFolder.list().length != 0) {
            for (File folder : new File(resultFolderpath).listFiles())
                if (folder.isDirectory())
                    simulationMenuA.getItems().add(folder.getName());

            simulationMenuA.getSelectionModel().select(0);
        }
    }

    /**
     *
     * @param file
     * @param guiController
     * @return
     */
    protected abstract MeteorologicFileTask
        newMeteorologicFileTask(File file, GuiController guiController);

    /**
     *
     * @param input
     * @param guiController
     * @return
     */
    protected abstract SimulationTask
    newSimulationTask(SimulationInput input, GuiController guiController);

    /**
     *
     * @param input
     * @param guiController
     * @return
     */
    protected abstract VisualizationTask
    newVisualizationTask(Tuple<SerializableSimulation, List<SerializableSimulation>> input, GuiController guiController);

    /*******************************************************************************

                            Acoes do modelo/dados

    *******************************************************************************/

    /**
     *
     * @param event
     */
    @FXML
    private void insertDataAction (MouseEvent event)
    {
        File selectedFile = getMeteoroloficFile();
        if (selectedFile != null) {
            MeteorologicFileTask meteorologicTask;
            meteorologicTask = newMeteorologicFileTask(selectedFile, this);
            meteorologicTask.execute();
        }
    }

    /**
     *
     * @param event
     */
    @FXML
    private void runAction (MouseEvent event)
    {
        SimulationTask simulationTask = newSimulationTask(getSimulationInput(), this);
        simulationTask.execute();
    }

    /**
     *
     */
    private void valueListeners ()
    {
        simulationMenuA.getSelectionModel().selectedItemProperty().addListener(
                (ov, t, newValue) -> {
            String simulationFolderName = simulationMenuA.getSelectionModel().getSelectedItem();
            if(simulationFolderName != null) {
                String SimulationPath = resultFolderpath + s + simulationFolderName;
                UnserializeSimulationTask unserializeSimulationTask;
                unserializeSimulationTask = new UnserializeSimulationTask(SimulationPath, this);
                unserializeSimulationTask.execute();
            }
        });

        simulationYearA.getSelectionModel().selectedItemProperty().addListener(
                (ov, t, newValue) -> {
            Integer index = simulationYearA.getSelectionModel().getSelectedIndex();
            if(index<0 || index > currentSimulation.size()-1) index = 0;
            SerializableSimulation selectedYear = currentSimulation.get(index);
            VisualizationTask visualizationTask = newVisualizationTask(new Tuple<>(selectedYear, currentSimulation),this);
            visualizationTask.execute();
        });

        Listenners();
    }

    /**
     *
     */
    protected abstract void Listenners();

    /**
     *
     * @param warning
     * @param from
     * @param to
     */
    public void warningNature (String warning, Color from, Color to)
    {
        tip.setText(warning);
        AnimationEvents.setWarning(warningButton, from, to);
        alertTip(null);
    }

/*******************************************************************************

                            Acoes da interface

*******************************************************************************/


    /**
     *
     * @param event
     */
    @FXML
    private void settingsAction (Event event)
    {
        if(Util.rb.equals(Util.en)) {
            configStage = newWindow(configStage, Util.fxmlLoaderC, Util.configFxml,
                    "cropModelsUFSM Configurations", null);
        } else {
            configStage = newWindow(configStage, Util.fxmlLoaderC, Util.configFxml,
                    "cropModelsUFSM Configurations", null);
        }
    }

    /**
     *
     * @param event
     */
    @FXML
    private void legendAction (Event event)
    {
        if(Util.rb.equals(Util.en)) {
            legendStage =newWindow(legendStage, Util.fxmlLoaderL, Util.legendFxml,
                    "cropModelsUFSM Legend", Util.legendHtml_EN);
        } else {
            legendStage = newWindow(legendStage, Util.fxmlLoaderL, Util.legendFxml,
                    "cropModelsUFSM Legend", Util.legendHtml_PT);
        }
    }

    /**
     *
     * @param event
     */
    @FXML
    private void aboutAction (Event event)
    {
        if(Util.rb.equals(Util.en)) {
            aboutStage = newWindow(aboutStage, Util.aboutFxmlLoader, Util.aboutFxml,
                    "cropModelsUFSM About", Util.aboutHtml_EN);
        } else {
            aboutStage = newWindow(aboutStage, Util.aboutFxmlLoader, Util.aboutFxml,
                    "cropModelsUFSM About", Util.aboutHtml_PT);
        }
    }

    /**
     *
     * @param event
     */
    @FXML
    private void helpAction (Event event)
    {
        if(Util.rb.equals(Util.en)) {
            helpStage = newWindow(helpStage, Util.HelpFxmlLoader, Util.helpFxml,
                    "cropModelsUFSM Manual", Util.helpHtml_EN);
        } else {
            helpStage = newWindow(helpStage, Util.HelpFxmlLoader, Util.helpFxml,
                    "cropModelsUFSM Manual", Util.helpHtml_PT);
        }
    }

    /**
     *
     * @param event
     */
    @FXML
    private void changePaneAction (MouseEvent event)
    {
        String label = ((ToggleButton)event.getSource() ).getText();

        if (label.contains("SIMULA"))
        {
            simulation.setVisible(true);
            tables.setVisible(false);
            charts.setVisible(false);
        }
        if(label.contains("TAB")){
            simulation.setVisible(false);
            tables.setVisible(true);
            charts.setVisible(false);
        }
        if(label.contains("CHART") || label.contains("GRÁF")){
            simulation.setVisible(false);
            tables.setVisible(false);
            charts.setVisible(true);
        }
        if(label.contains("METEORO")){
            meteorologic.setVisible(true);
            meteorologicLegend.setVisible(true);
            mass.setVisible(false);
            massLegend.setVisible(false);
            development.setVisible(false);
            developmentLegend.setVisible(false);
        }
        if(label.contains("DESENV") || label.contains("DEVELOP")){
            meteorologic.setVisible(false);
            meteorologicLegend.setVisible(false);
            mass.setVisible(false);
            massLegend.setVisible(false);
            development.setVisible(true);
            developmentLegend.setVisible(true);
        }
        if(label.contains("CRESC") || label.contains("GROW")) {
            meteorologic.setVisible(false);
            meteorologicLegend.setVisible(false);
            development.setVisible(false);
            developmentLegend.setVisible(false);
            mass.setVisible(true);
            massLegend.setVisible(true);
        }
    }

    /**
     *
     * @param event
     */
    @FXML
    protected void maximizeAction (MouseEvent event)
    {
        if(stage.isMaximized()) {
            galeryViewport = new Rectangle2D((1920 - minWidth)/2, 0, minWidth, 150.0);
            upImv.setViewport(galeryViewport);
            upImv.setFitWidth(minWidth);
            downImv.setViewport(galeryViewport);
            downImv.setFitWidth(minWidth);
            video.setViewport(galeryViewport);
            video.setFitWidth(minWidth);
        }
        else {
            galeryViewport = new Rectangle2D(0, 0, maxWidth, 150.0);
            upImv.setViewport(galeryViewport);
            upImv.setFitWidth(maxWidth);
            downImv.setViewport(galeryViewport);
            downImv.setFitWidth(maxWidth);
            video.setViewport(galeryViewport);
            video.setFitWidth(maxWidth);
        }
        super.maximizeAction(event);
    }

    /**
     *
     * @param event
     */
    @FXML
    private void alertTip (MouseEvent event)
    {
        tip.setFont(Font.font("Roboto", 15));
        if(!tip.isShowing()) tip.show(stage);
        else                 tip.hide();
    }

    /***************************************************************************

                                subacoes da classe

     **************************************************************************/

    /**
     *
     * @return
     */
    private File getMeteoroloficFile ()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(rb.getString("key7"));
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Text Files", "*.txt"));
        return fileChooser.showOpenDialog(stage);
    }

    /**
     *
     * @param warning
     */
    private static void showWarning (String warning)
    {
        System.out.println(warning);
        warningStage = warn(warningStage, Util.WarningFxmlLoader, Util.warningFxml,
                    "cropModelsUFSM Manual", warning);
    }

    /**
     *
     * @param stage
     * @param loader
     * @param fxml
     * @param winTitle
     * @param url
     * @return
     */
    private Stage newWindow (Stage stage, FXMLLoader loader, InputStream fxml,
                            String winTitle, String url)
    {
        try {
            WebPanesFXController controller = null;
            if (stage == null) {
                Group group = (Group) loader.load(fxml);
                Scene scene = new Scene(group);
                scene.setFill(Color.TRANSPARENT);
                stage = new Stage();
                stage.getIcons().add(Util.icon);
                stage.setTitle(winTitle);
                stage.setScene(scene);
                stage.initStyle(StageStyle.TRANSPARENT);
                controller = loader.getController();
                controller.postInitializeTasks(stage);
                controller.loadWebContent(url);
                stage.show();
            } else {
                if (stage.isShowing())
                    stage.hide();
                else

                    controller = loader.getController();
                    controller.postInitializeTasks(stage);
                    controller.loadWebContent(url);
                    stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }

    /**
     *
     * @param stage
     * @param loader
     * @param fxml
     * @param winTitle
     * @param warn
     * @return
     */
    private static Stage warn (Stage stage, FXMLLoader loader, InputStream fxml,
                             String winTitle, String warn)
    {
        try {
            WarningFXController controller = null;
            if (stage == null) {
                Group group = (Group) loader.load(fxml);
                Scene scene = new Scene(group);
                scene.setFill(Color.TRANSPARENT);
                stage = new Stage();
                stage.getIcons().add(Util.icon);
                stage.setTitle(winTitle);
                stage.setScene(scene);
                stage.initStyle(StageStyle.TRANSPARENT);
                controller = loader.getController();
                controller.postInitializeTasks(stage);
                controller.loadWarning(warn);
                stage.show();
            } else {
                if (stage.isShowing())
                    stage.hide();
                else
                    controller = loader.getController();
                controller.postInitializeTasks(stage);
                controller.loadWarning(warn);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }

    /**
     *
     * @param newValue
     * @param chart
     * @param series
     * @param index
     */
    public void changeChartDataVisibility (Boolean newValue, LineChart chart,
                                            String series, int index)
    {
        List<Node> nodes = new ArrayList<Node>(chart.lookupAll(series));
        List<Node> label = new ArrayList<Node>
                (chart.lookupAll(".chart-legend-item"));
        List<Node> symbol = new ArrayList<Node>
                (chart.lookupAll(".chart-legend-item-symbol"));
        if(!newValue){
            nodes.forEach(n -> n.setVisible(false));
            label.get(index).setVisible(false);
            symbol.get(index).setVisible(false);
        } else {
            nodes.forEach(n -> n.setVisible(true));
            label.get(index).setVisible(true);
            symbol.get(index).setVisible(true);
        }
    }

    /**
     *
     * @param currentTaskSimulation
     */
    protected abstract void setSimulationOnGui(VisualizableSimulation currentTaskSimulation);

    /**
     *
     * @return
     */
    protected abstract SimulationInput getSimulationInput();

    /**
     *
     * @param simulationName
     */
    private void updateSimulationMenus(String simulationName)
    {
        simulationMenuA.getItems().clear();
        if(new File(resultFolderpath).exists()) {
            for (File folder : new File(resultFolderpath).listFiles())
                if (folder.isDirectory())
                    simulationMenuA.getItems().add(folder.getName());

            simulationMenuA.getSelectionModel().select(simulationName);
            updateYearsMenu();
        }
    }

    /**
     *
     */
    private void updateYearsMenu()
    {
        simulationYearA.getItems().clear();
        currentSimulation.forEach(s -> {
            simulationYearA.getItems().add(String.valueOf(s.getYear()));
        });
        simulationYearA.getSelectionModel().select(0);
    }

    /**
     *
     * @param thisTask
     */
    @Override
    public void acceptTask(Task thisTask)
    {
        Platform.runLater(() -> {
            SerializableSimulation firstYear;
            if(thisTask instanceof SimulationTask){
                currentSimulation = ((SimulationTask) thisTask).getOutput();
                firstYear = currentSimulation.get(0);
                VisualizationTask visualisationTask;
                visualisationTask = newVisualizationTask(new Tuple<>(firstYear,currentSimulation),this);
                visualisationTask.execute();
                try {
                    updateSimulationMenus(Util.generateSimulationName(firstYear.getSimulationInput()));
                } catch (Exception e) {
                    logger.log(Level.SEVERE , e.getMessage(), e);
                }
                AnimationEvents.simulationSucess(simulationButtonCircle, okImv, playImv,
                        Color.web(getText(134)), Color.web("#43E186"));
            }
            if(thisTask instanceof VisualizationTask){
                setSimulationOnGui(((VisualizationTask) thisTask).getOutput());
            }
            if(thisTask instanceof MeteorologicFileTask) {
                AnimationEvents.setDataRange(Duration.seconds(0.5), 1, dataLabel,
                        dataShape, Color.web("#D6462F"), Color.web("#43E186"),
                        ((MeteorologicFileTask) thisTask).getOutput());
            }
            if(thisTask instanceof UnserializeSimulationTask) {
                currentSimulation = ((UnserializeSimulationTask) thisTask).getOutput();
                updateYearsMenu();
            }
        });
    }


    /**
     *
     * @param thisTask
     * @param warning
     */
    @Override
    public void regectTask(Task thisTask, String warning)
    {
        Platform.runLater(() -> showWarning(warning));
    }
}
