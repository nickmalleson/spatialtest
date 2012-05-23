/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package andresenspatialtest;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.factory.GeoTools;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.MultiSplitLayout;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

/**
 *
 * @author Nick Malleson
 */
public class SpatialTestGUIv2 extends JFrame {

//   private static final int GUI_WIDTH = 1024;
   private static final int GUI_WIDTH = 1200;
   private static final int GUI_HEIGHT = 768;
   private JXMultiSplitPane mainPane; // The main container
   // Panels to group similar tasks
   private JXTaskPaneContainer taskContainer;
   private JXTaskPane fileTaskGroup;
   private JXTaskPane paramsTaskGroup;
   private JXTaskPane runTaskGroup;
   // The output window and progress bar
   private JTextArea console;
   private JScrollPane consoleScrollPane;
   // For actually writing to the console
   private ConsoleWriter consoleWriter = new SimpleConsoleWriter();
   private JProgressBar progressBar;
   // The map window
   private MapContext map;
   private MapWindow mapWindow;
   // Components for loading/saving files
   private JTextField baseDataText = new JTextField("Select Base Data");
   private JTextField testDataText = new JTextField("Select Test Data");
   private JTextField areaDataText = new JTextField("Select Area Data");
   private JTextField outputDataText = new JTextField("Select Output Data");
   // Array to simplify operations that apply to each text field
   private JTextField[] browseButtonTextFields =
           new JTextField[]{baseDataText, testDataText, areaDataText, outputDataText};
   private File baseFile = null;
   private File testFile = null;
   private File areaFile = null;
   private File outputAreaFile = null;
   private JButton browseBaseFile = new JButton("Browse Base Data");
   private JButton browseTestFile = new JButton("Browse Test Data");
   private JButton browseAreaFile = new JButton("Browse Area Data");
   private JButton browseOutputFile = new JButton("Browse Output Data");
   // Array to simplify operations that apply to each button
   private JButton[] browsButtons =
           new JButton[]{browseBaseFile, browseTestFile, browseAreaFile, browseOutputFile};
   // Always use same file chooser throughout program (remembers current dir etc)
   private JFileChooser chooser;
   // Model parameters
   private int monteCarlo = 100;
   private int sampleSizePct = 85;
   private int confidenceInterval = 95;
   private JTextField monteCarloText = new JTextField(monteCarlo + "");
   private JTextField sampleSizePctText = new JTextField(sampleSizePct + "");
   private JTextField confidenceIntervalText = new JTextField(confidenceInterval + "");
   private JLabel monteCarloLabel = new JLabel("Number of iterations");
   private JLabel sampleSizePctLabel = new JLabel("Sample size (%)");
   private JLabel confidenceIntervalLabel = new JLabel("Confidence Interval (%)");
   private JButton runButton = new JButton("Run");

   public SpatialTestGUIv2() {
      initComponents();
   }

   public static void main(String args[]) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (ClassNotFoundException ex) {
         Logger.getLogger(SpatialTestGUIv2.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (InstantiationException ex) {
         Logger.getLogger(SpatialTestGUIv2.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (IllegalAccessException ex) {
         Logger.getLogger(SpatialTestGUIv2.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (UnsupportedLookAndFeelException ex) {

         Logger.getLogger(SpatialTestGUIv2.class.getName()).log(Level.SEVERE, null, ex);
      }

      java.awt.EventQueue.invokeLater(new Runnable() {

         public void run() {

            new SpatialTestGUIv2().setVisible(true);
         }

      });

      System.out.println("Geotools version: " + GeoTools.getVersion());
   }

   private void initComponents() {

      // Initialise the frame
      this.setLayout(new BorderLayout());
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setPreferredSize(new Dimension(GUI_WIDTH, GUI_HEIGHT));
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(true);

      //  ***** Create the task groups panels *****

      this.taskContainer = new JXTaskPaneContainer();

      this.fileTaskGroup = new JXTaskPane();
      this.fileTaskGroup.setName("fileGroup");
      this.fileTaskGroup.setTitle("Files");
      this.taskContainer.add(this.fileTaskGroup);

      this.paramsTaskGroup = new JXTaskPane();
      this.paramsTaskGroup.setName("paramsGroup");
      this.paramsTaskGroup.setTitle("Model Parameters");
      this.taskContainer.add(this.paramsTaskGroup);

      this.runTaskGroup = new JXTaskPane();
      this.runTaskGroup.setName("runGroup");
      this.runTaskGroup.setTitle("Run Model");
      this.taskContainer.add(this.runTaskGroup);

      //  ***** Create the panel for the console and the progress bar *****

      JPanel consolePanel = new JPanel();
      BoxLayout bl = new BoxLayout(consolePanel, BoxLayout.Y_AXIS);
      consolePanel.setLayout(bl);

      this.console = new JTextArea(" ");
      this.console.setEditable(false);
      this.console.setFont(new Font("Monospaced", Font.PLAIN, 12));
      this.console.setLineWrap(true);
      this.console.setWrapStyleWord(true);
      this.console.setForeground(Color.green);
      this.console.setBackground(Color.black);

      this.consoleScrollPane = new JScrollPane(this.console);
      consolePanel.add(consoleScrollPane);

      this.progressBar = new JProgressBar();

      consolePanel.add(this.progressBar);

      //  ***** Create the file selection boxes *****


      // This commented stuff adds the buttons and text to panels,
      // doesn't work as well as adding them directly, looks funny.
//      final int BUTTON_WIDTH = 200;
//      final int TEXT_WIDTH = 200;
//      JPanel baseP = new JPanel(new FlowLayout());
//      baseP.add(this.baseDataText);
//      baseP.add(this.browseBaseFile);
//      this.browseBaseFile.setPreferredSize(new Dimension(BUTTON_WIDTH, 25));
//      this.baseDataText.setPreferredSize(new Dimension(TEXT_WIDTH, 25));

//      JPanel testP = new JPanel(new FlowLayout());
//      testP.add(this.testDataText);
//      testP.add(this.browseTestFile);
//      this.browseTestFile.setPreferredSize(new Dimension(BUTTON_WIDTH, 25));
//      this.testDataText.setPreferredSize(new Dimension(TEXT_WIDTH, 25));

//      JPanel areaP = new JPanel(new FlowLayout());
//      areaP.add(this.areaDataText);
//      areaP.add(this.browseAreaFile);
//      this.browseAreaFile.setPreferredSize(new Dimension(BUTTON_WIDTH, 25));
//      this.areaDataText.setPreferredSize(new Dimension(TEXT_WIDTH, 25));

//      JPanel outputP = new JPanel(new FlowLayout());
//      outputP.add(this.outputDataText);
//      outputP.add(this.browseOutputFile);
//      this.browseOutputFile.setPreferredSize(new Dimension(BUTTON_WIDTH, 25));
//      this.areaDataText.setPreferredSize(new Dimension(TEXT_WIDTH, 25));

      this.fileTaskGroup.add(this.baseDataText, 0);
      this.fileTaskGroup.add(this.browseBaseFile, 1);
      this.fileTaskGroup.add(this.testDataText, 2);
      this.fileTaskGroup.add(this.browseTestFile, 3);
      this.fileTaskGroup.add(this.areaDataText, 4);
      this.fileTaskGroup.add(this.browseAreaFile, 5);
      this.fileTaskGroup.add(this.outputDataText, 6);
      this.fileTaskGroup.add(this.browseOutputFile, 7);

//      this.fileTaskGroup.add(baseP, 0);
//      this.fileTaskGroup.add(testP, 1);
//      this.fileTaskGroup.add(areaP, 2);
//      this.fileTaskGroup.add(outputP, 3);

      // Actions for the browse buttons
      for (int i = 0; i < browsButtons.length; i++) {
         browsButtons[i].addActionListener(new BrowseButtonActionListener());
      }

      //  ***** Add the model parameter selection boxes *****
      this.paramsTaskGroup.add(this.monteCarloLabel);
      this.paramsTaskGroup.add(this.monteCarloText);
      this.paramsTaskGroup.add(this.sampleSizePctLabel);
      this.paramsTaskGroup.add(this.sampleSizePctText);
      this.paramsTaskGroup.add(this.confidenceIntervalLabel);
      this.paramsTaskGroup.add(this.confidenceIntervalText);

      // Need listeners to listen for changes to the parameter text boxes
      this.monteCarloText.getDocument().addDocumentListener(new NumberListener(
              this.monteCarloText,
              new SetMethod() {

                 public void set(int i) {
                    SpatialTestGUIv2.this.monteCarlo = i;
                 }

                 public String fieldName() {
                    return "number of iterations";
                 }

              }));

      this.sampleSizePctText.getDocument().addDocumentListener(new NumberListener(
              this.sampleSizePctText,
              new SetMethod() {

                 public void set(int i) {
                    SpatialTestGUIv2.this.sampleSizePct = i;
                 }

                 public String fieldName() {
                    return "sample size";
                 }

              }));

      this.confidenceIntervalText.getDocument().addDocumentListener(new NumberListener(
              this.confidenceIntervalText,
              new SetMethod() {

                 public void set(int i) {
                    SpatialTestGUIv2.this.confidenceInterval = i;
                 }

                 public String fieldName() {
                    return "confidence interval";
                 }

              }));

      // ***** Create the run botton etc*****

      this.runButton.setBackground(Color.red);
      this.runButton.addActionListener(new ActionListener() {

         public void actionPerformed(ActionEvent e) {
            SpatialTestGUIv2.this.runModel();
         }

      });
      this.runTaskGroup.add(this.runButton);

      //  ***** Create the map panel to display data and results *****
//      this.mapPane = new JMapPane();
      this.mapWindow = new MapWindow();
      this.mapWindow.setBackground(Color.WHITE);
      this.mapWindow.setRenderer(new StreamingRenderer());
      this.mapWindow.showMap();



      //  ***** Create the main panel *****

      this.mainPane = new JXMultiSplitPane();
      String layoutDef =
              "(COLUMN (ROW weight=0.6 "
              + "(LEAF name=left weight=0.2) "
              + "(LEAF name=right weight=0.8) "
              + ")"
              + "(LEAF name=bottom weight=0.4) )";

      MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel(layoutDef);
      this.mainPane.getMultiSplitLayout().setModel(modelRoot);

      // Add components to the panel

      this.mainPane.add(this.taskContainer, "left");
      this.mainPane.add(this.mapWindow, "right");
      this.mainPane.add(consolePanel, "bottom");

      // ADDING A BORDER TO THE MULTISPLITPANE CAUSES ALL SORTS OF ISSUES
      this.mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

      this.getContentPane().add(this.mainPane);
   }

   private void createFileChooser() {
      if (this.chooser == null) {
         this.chooser = new JFileChooser(".");
         ExampleFileFilter filter = new ExampleFileFilter();
         filter.addExtension("shp");
         filter.setDescription("ESRI Shapefiless");
         chooser.setFileFilter(filter);
      }
   }

   /**
    * http://docs.geotools.org/stable/userguide/examples/stylefunctionlab.html
    * @param f
    */
   private void displayShapefile(File f) {

      // Map might not have been initialised yet
      if (this.map == null) {
         this.map = new DefaultMapContext();
         this.map.setTitle("Spatial Data");
      }

      // Create a FeatureSource from the shapefile
      FeatureSource featureSource = null;
      try {
         FileDataStore store = FileDataStoreFinder.getDataStore(f);
         featureSource = store.getFeatureSource();

      }
      catch (IOException ex) {
         SpatialTestGUIv2.this.consoleWriter.writeToConsole("Could not display the "
                 + "file '" + f.getAbsolutePath() + "' due to an IOException: "
                 + ex.getMessage(), true);
         return;
      }

      // See if the data has the particular field we're interested in ('results')
      boolean containsColumn = false;
      FeatureType type = featureSource.getSchema();
      for (PropertyDescriptor desc : type.getDescriptors()) {
         if (desc.getName().getLocalPart().equals(SpatialTestAlgv2.getSIndexColumnName())) {
            containsColumn = true;
            break;
         }
      } // for

      Style style = null;
      try {
         if (containsColumn) {
            // Passing the featureSource tells the MapWindow that it should colour
            // the map according to the value of the "SIndex" column.
            MapWindow.createDefaultStyle(featureSource, SpatialTestAlgv2.getSIndexColumnName());
         }
         else {
            MapWindow.createDefaultStyle(featureSource, null);
         }
      }
      catch (IOException ex) {
         this.consoleWriter.writeToConsole("There was an IOException trying to "
                 + "add a layer to the map. Message is: "
                 + (ex.getMessage() == null ? "<none>" : ex.getMessage()), true);

         this.consoleWriter.writeToConsole(ex.getStackTrace());
      }

      this.map.addLayer(featureSource, style);

      this.mapWindow.setMapContext(this.map);
      this.mapWindow.setSize(400, 400);
//      this.mapPane.reset();
      this.mapWindow.setVisible(true);

   }

   /**
    * The function that is in charge of running the program. Checks all
    * parameters are correct and then runs the alg in a new thread (using the
    * ModelRunner class). While the alg is running the GUI stays active so that
    * console messages can be printed. Once the algorithm has finished the ModelRunner
    * calls the this.algFinished() funcion so that the GUI can display results
    * etc.
    */
   private void runModel() {

      if (this.monteCarlo < 1) {
         JOptionPane.showMessageDialog(this, "Please set the number of iterations  "
                 + "to be greater than 1");
         return;
      }
      else if (this.sampleSizePct < 1 || this.sampleSizePct > 100) {
         JOptionPane.showMessageDialog(this, "Please enter a sample size that "
                 + "is a number (integer) between 1 and 100");
         return;
      }
      else if (this.confidenceInterval < 1 || this.confidenceInterval > 100) {
         JOptionPane.showMessageDialog(this, "Please enter a confidence interval that "
                 + "is a number (integer) between 1 and 100");
         return;
      }

      if (this.baseFile == null || this.testFile == null
              || this.areaFile == null || this.outputAreaFile == null) {
         JOptionPane.showMessageDialog(this, "One of the input files has not been"
                 + " selected yet.");
         return;
      }

      SpatialTestAlgv2 st = new SpatialTestAlgv2();
      st.setBaseShapefile(baseFile);
      st.setTestShapefile(testFile);
      st.setAreaShapefile(areaFile);
      st.setOutputShapefile(outputAreaFile);
      st.setMonteCarlo(monteCarlo);
      st.setSamplePercentage(sampleSizePct);
      st.setConfidenceInterval(confidenceInterval);

      // Tell the algorithm to write to the GUI console, making the text red
      // if it represents an error
      st.setConsole(new SimpleConsoleWriter());

      try {
         // Run the algorithm in a new thread.
         ModelRunner r = new ModelRunner(st, this);
         r.start();
         JOptionPane.showMessageDialog(this, "Model is running");
      }
      catch (Exception e) {
         this.consoleWriter.writeToConsole("There was an error running the "
                 + "spatial test algoruthm: "
                 + e.getMessage() == null ? "<no message>" : e.getMessage(), true);
         this.consoleWriter.writeToConsole(e.getStackTrace());

      }
      // Unless there was an error, the model will have started now, when it has
      // finished it will call the algFinished() function.

   }

   /**
    * Called by the ModelRunner class (which runs the algorithm in a different
    * thread) when it has finished so that the GUI can display results etc.
    * @param mr The ModelRunner that ran the algorithm (in a new thread)
    */
   public void algFinished(ModelRunner mr) {

      boolean success = mr.isSuccess();
      SpatialTestAlgv2 alg = mr.getAlg();

      if (success) {
         JOptionPane.showMessageDialog(this, "All finished successfully.\n"
                 + "Found global S value: " + alg.getGlobalS() + "\n"
                 + "Output areas file written to:\n\t"
                 + this.outputAreaFile.getAbsoluteFile().toString() + "");
      }
      else {
         JOptionPane.showMessageDialog(this, "There was an error running the algorithm.");
      }

      // Display the results
      this.displayShapefile(this.outputAreaFile);
   }

   /** Class for events that are called when the user presses one of the
    *  file browser buttons. */
   class BrowseButtonActionListener implements ActionListener {

      public void actionPerformed(ActionEvent e) {
         SpatialTestGUIv2.this.createFileChooser(); // Create the file chooser (if it is null)
         // Choose the file
         SpatialTestGUIv2.this.chooser.setDialogTitle("Choose a shapefile");
         int returnVal = 0;
         // Need to check whether to show the 'browse' or 'save' dialogue (for the results file)
         if (e.getSource().equals(SpatialTestGUIv2.this.browseOutputFile)) {
            returnVal = chooser.showSaveDialog(SpatialTestGUIv2.this);
         }
         else {
            returnVal = chooser.showOpenDialog(SpatialTestGUIv2.this);
         }

         // Check a file was selected
         if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();

            // Work out which button was pressed so the appropriate file can be set
            if (e.getSource().equals(SpatialTestGUIv2.this.browseBaseFile)) {
               SpatialTestGUIv2.this.baseFile = f;
               SpatialTestGUIv2.this.baseDataText.setText(f.getName());
               SpatialTestGUIv2.this.baseDataText.setBackground(Color.GREEN);
               SpatialTestGUIv2.this.consoleWriter.writeToConsole("Selected base file: " + f.getName(), false);
            }
            else if (e.getSource().equals(SpatialTestGUIv2.this.browseTestFile)) {
               SpatialTestGUIv2.this.testFile = f;
               SpatialTestGUIv2.this.testDataText.setText(f.getName());
               SpatialTestGUIv2.this.testDataText.setBackground(Color.GREEN);
               SpatialTestGUIv2.this.consoleWriter.writeToConsole("Selected test file: " + f.getName(), false);
            }
            else if (e.getSource().equals(SpatialTestGUIv2.this.browseAreaFile)) {
               SpatialTestGUIv2.this.areaFile = f;
               SpatialTestGUIv2.this.areaDataText.setText(f.getName());
               SpatialTestGUIv2.this.areaDataText.setBackground(Color.GREEN);
               SpatialTestGUIv2.this.consoleWriter.writeToConsole("Selected area file: " + f.getName(), false);
            }
            else if (e.getSource().equals(SpatialTestGUIv2.this.browseOutputFile)) {
               // For the output file, need to make sure it ends in ".shp"
               String name = f.getName();
               // See if the file ends in .shp (or is less than 5 characters long, in which case it
               // definitely can't
               if ( !name.endsWith(".shp") )
               {
                  f = new File(f.getAbsolutePath() + ".shp");
               }
               SpatialTestGUIv2.this.outputAreaFile = f;
               SpatialTestGUIv2.this.outputDataText.setText(name);
               SpatialTestGUIv2.this.outputDataText.setBackground(Color.GREEN);
               SpatialTestGUIv2.this.consoleWriter.writeToConsole("Selected output file: " + f.getName(), false);


            }
            else {
               assert true : "Unrecognised action in button action listener: " + e.toString();
            }

            // Add the data to the map (unless it's the output file, this won't have been created yet
            if (!e.getSource().equals(SpatialTestGUIv2.this.browseOutputFile)) {
               SpatialTestGUIv2.this.displayShapefile(f);
            }

         } // if returnVal is OK
      } // actionPerformed

   } // BrowseButtonActionListener class

   /**
    * Used for writing to the console
    */
   class SimpleConsoleWriter implements ConsoleWriter {

      public void writeToConsole(String text, boolean error) {
         if (error) {
            SpatialTestGUIv2.this.console.setForeground(Color.RED);
            SpatialTestGUIv2.this.console.append(text + "\n");
            SpatialTestGUIv2.this.console.setForeground(Color.GREEN);
            System.err.println(text);
         }
         else {
            SpatialTestGUIv2.this.console.append(text + "\n");
            System.out.println(text);
         }
         SpatialTestGUIv2.this.console.setCaretPosition(
                 SpatialTestGUIv2.this.console.getDocument().getLength());
      }

      public void writeToConsole(StackTraceElement[] stackTrace) {
         SpatialTestGUIv2.this.console.setForeground(Color.RED);
         for (StackTraceElement e : stackTrace) {
            SpatialTestGUIv2.this.console.append(e.toString() + "\n");
         }
         SpatialTestGUIv2.this.console.setForeground(Color.GREEN);
      }

   }

   /**
    * Listens for changes to a textfield and makes sure that the text can be converted
    * into an Integer.
    */
   class NumberListener implements DocumentListener {

      private JTextField tf;
      private SetMethod setMethod;

      /**
       * Create a listner that checks the text of a given textfield is an
       * integer and is greater than 0.
       *
       * @param tf The text field to listen to numbers on
       * @param setMethod An objevt that provides a means of accessing the
       * primitive that the text field needs to change. This is massively
       * over-complicated really, but it avoids the need for an if-else
       * statement to check whether the monte-carlo number or the sample size is
       * being passed.
       */
      public NumberListener(JTextField tf, SetMethod setMethod) {
         this.tf = tf;
         this.setMethod = setMethod;
      }

      public void insertUpdate(DocumentEvent e) {
         checkUpdate(e);
      }

      public void removeUpdate(DocumentEvent e) {
         checkUpdate(e);
      }

      public void changedUpdate(DocumentEvent e) {
         checkUpdate(e);
      }

      private void checkUpdate(DocumentEvent e) {
         try {
            Integer i = Integer.parseInt(tf.getText());
            if (i < 1) {
               throw new Exception();
            }
            tf.setBackground(Color.GREEN);
            setMethod.set(i);
            SpatialTestGUIv2.this.consoleWriter.writeToConsole("Have set " + setMethod.fieldName()
                    + " to: " + tf.getText(), false);

         }
         catch (NumberFormatException ex) {
            SpatialTestGUIv2.this.consoleWriter.writeToConsole(setMethod.fieldName()
                    + " must be an integer, not: " + tf.getText(), false);
            tf.setBackground(Color.RED);
            setMethod.set(-1); // Set to -1 so that we know the value is invalid
         }
         catch (Exception ex) {
            SpatialTestGUIv2.this.consoleWriter.writeToConsole(setMethod.fieldName()
                    + " must be greater than 0: " + tf.getText(), false);
            tf.setBackground(Color.RED);
            setMethod.set(-1); // Set to -1 so that we know the value is invalid
         }
      }

   }

   /** Provides a means of passing setting a primitive and printing a message.
    * <p>
    * This is massively over-complicated really, but it avoids the need for an if-else
    * in the <code>NumberListener</code> class.
    * @see NumberListener
    */
   interface SetMethod {

      public void set(int i);

      public String fieldName();

   }

   class ModelRunner extends Thread {

      private SpatialTestAlgv2 st;
      private SpatialTestGUIv2 gui;
      private boolean success;

      public ModelRunner(SpatialTestAlgv2 st, SpatialTestGUIv2 gui) {
         this.st = st;
         this.gui = gui;
         success = false;
      }

      @Override
      public void run() {
         success = st.runAlgorithm();
         // Tell the GUI that the algorithm has finished
         gui.algFinished(this);
      }

      public boolean isSuccess() {
         return this.success;
      }

      /**
       * The GUI needs a way to find the algorithm object again
       * @return
       */
      public SpatialTestAlgv2 getAlg() {
         return this.st;
      }

   }

}

