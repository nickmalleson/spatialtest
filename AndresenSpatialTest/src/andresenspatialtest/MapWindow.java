/*
 * This  is a file that I have copied verbatim from GeoTools source
 * and made some small edits to make it work within a larger window.
 *
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package andresenspatialtest;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import net.miginfocom.swing.MigLayout;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.FunctionExpressionImpl;

import org.geotools.map.MapContext;
import org.geotools.renderer.GTRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapPane;
import org.geotools.swing.MapLayerTable;
import org.geotools.swing.StatusBar;
import org.geotools.swing.action.InfoAction;
import org.geotools.swing.action.PanAction;
import org.geotools.swing.action.ResetAction;
import org.geotools.swing.action.ZoomInAction;
import org.geotools.swing.action.ZoomOutAction;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * This is a class that I have created using the GeoTools JMapFrame source code.
 * I've made a few small changes so that the map window can be displayed within
 * an enclosing frame (otherwise the class is its own frame and therefore
 * cannot be embedded in higher containers). Otherwise the source is basically
 * unchanged from the original and all credit should go to the original author
 * (Michael Bedward I think).
 *
 * I added a couple of functions (also from GeoTools source) to create Styles
 * for map layers.
 *
 * @see MapLayerTable
 * @see StatusBar
 *
 * @author Michael Bedward
 * @author Nick Malleson
 *
 * @source $URL: http://svn.osgeo.org/geotools/trunk/modules/unsupported/swing/src/main/java/org/geotools/swing/JMapFrame.java $
 */
public class MapWindow extends JPanel {

   private static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
   private static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
   private static FilterFactory2 filterFactory2 = CommonFactoryFinder.getFilterFactory2(null);

   /**
    * Constants for available toolbar buttons used with the
    * {@linkplain #enableTool} method.
    */
   public enum Tool {

      /**
       * Used to request that an empty toolbar be created
       */
      NONE,
      /**
       * Requests the feature info cursor tool
       */
      INFO,
      /**
       * Requests the pan cursor tool
       */
      PAN,
      /**
       * Requests the reset map extent cursor tool
       */
      RESET,
      /**
       * Requests the zoom in and out cursor tools
       */
      ZOOM;
   }

   private Set<Tool> toolSet;

   /*
    * UI elements
    */
   private JMapPane mapPane;
   private MapLayerTable mapLayerTable;
   private JToolBar toolBar;
   private StatusBar statusBar;
   private boolean showStatusBar;
   private boolean showLayerTable;
   private boolean uiSet;

   /**
    * Creates a new {@code JMapFrame} object with a toolbar, map pane and status
    * bar; sets the supplied {@code MapContext}; and displays the frame on the
    * AWT event dispatching thread. The context's title is used as the frame's
    * title.
    *
    * @param context the map context containing the layers to display
    */
   public void showMap() {

      this.enableStatusBar(true);
      this.enableToolBar(true);
      this.initComponents();
      this.setSize(400, 400);
//       this.setMapContext(context);
      this.setVisible(true);
   }

   public MapWindow() {

      showLayerTable = true;
      showStatusBar = true;
      toolSet = new HashSet<Tool>();

      // the map pane is the one element that is always displayed
      mapPane = new JMapPane();
      mapPane.setBackground(Color.WHITE);
//        mapPane.setSize(WIDTH, HEIGHT);
      mapPane.setPreferredSize(new Dimension(300, 500));
//        mapPane.setMapContext(context);
//        mapPane.setRenderer(renderer);

   }

   /**
    * Set whether a toolbar, with a basic set of map tools, will be displayed
    * (default is false). Calling this with state == true is equivalent to
    * calling {@linkplain #enableTool} with all {@linkplain JMapFrame.Tool}
    * constants.
    *
    * @param state whether the toolbar is required
    */
   public void enableToolBar(boolean state) {
      if (state) {
         toolSet.addAll(EnumSet.allOf(Tool.class));
      }
      else {
         toolSet.clear();
      }
   }

   /**
    * This method is an alternative to {@linkplain #enableToolBar(boolean)}.
    * It requests that a tool bar be created with specific tools, identified
    * by {@linkplain JMapFrame.Tool} constants.
    * <code><pre>
    * myMapFrame.enableTool(Tool.PAN, Tool.ZOOM);
    * </pre></code>
    *
    * @param tool one or more {@linkplain JMapFrame.Tool} constants
    */
   public void enableTool(Tool... tool) {
      for (Tool t : tool) {
         toolSet.add(t);
      }
   }

   /**
    * Set whether a status bar will be displayed to display cursor position
    * and map bounds.
    *
    * @param state whether the status bar is required.
    */
   public void enableStatusBar(boolean state) {
      showStatusBar = state;
   }

   /**
    * Set whether a map layer table will be displayed to show the list
    * of layers in the map context and set their order, visibility and
    * selected status.
    *
    * @param state whether the map layer table is required.
    */
   public void enableLayerTable(boolean state) {
      showLayerTable = state;
   }

   /**
    * Calls {@linkplain #initComponents()} if it has not already been called explicitly
    * to construct the frame's components before showing the frame.
    *
    * @param state true to show the frame; false to hide.
    */
   @Override
   public void setVisible(boolean state) {
      if (state && !uiSet) {
         initComponents();
      }

      super.setVisible(state);
   }

   /**
    * Creates and lays out the frame's components that have been
    * specified with the enable methods (e.g. {@linkplain #enableToolBar(boolean)} ).
    * If not called explicitly by the client this method will be invoked by
    * {@linkplain #setVisible(boolean) } when the frame is first shown.
    */
   public void initComponents() {
      if (uiSet) {
         // @todo log a warning ?
         return;
      }

      /*
       * We use the MigLayout manager to make it easy to manually code
       * our UI design
       */
      StringBuilder sb = new StringBuilder();
      if (!toolSet.isEmpty()) {
         sb.append("[]"); // fixed size
      }
      sb.append("[grow]"); // map pane and optionally layer table fill space
      if (showStatusBar) {
         sb.append("[30px::]"); // status bar height
      }

      JPanel panel = new JPanel(new MigLayout(
              "wrap 1, insets 0", // layout constrains: 1 component per row, no insets

              "[grow]", // column constraints: col grows when frame is resized

              sb.toString()));

      /*
       * A toolbar with buttons for zooming in, zooming out,
       * panning, and resetting the map to its full extent.
       * The cursor tool buttons (zooming and panning) are put
       * in a ButtonGroup.
       *
       * Note the use of the XXXAction objects which makes constructing
       * the tool bar buttons very simple.
       */
      if (!toolSet.isEmpty()) {
         toolBar = new JToolBar();
         toolBar.setOrientation(JToolBar.HORIZONTAL);
         toolBar.setFloatable(false);

         ButtonGroup cursorToolGrp = new ButtonGroup();

         if (toolSet.contains(Tool.ZOOM)) {
            JButton zoomInBtn = new JButton(new ZoomInAction(mapPane));
            toolBar.add(zoomInBtn);
            cursorToolGrp.add(zoomInBtn);

            JButton zoomOutBtn = new JButton(new ZoomOutAction(mapPane));
            toolBar.add(zoomOutBtn);
            cursorToolGrp.add(zoomOutBtn);

            toolBar.addSeparator();
         }

         if (toolSet.contains(Tool.PAN)) {
            JButton panBtn = new JButton(new PanAction(mapPane));
            toolBar.add(panBtn);
            cursorToolGrp.add(panBtn);

            toolBar.addSeparator();
         }

         if (toolSet.contains(Tool.INFO)) {
            JButton infoBtn = new JButton(new InfoAction(mapPane));
            toolBar.add(infoBtn);

            toolBar.addSeparator();
         }

         if (toolSet.contains(Tool.RESET)) {
            JButton resetBtn = new JButton(new ResetAction(mapPane));
            toolBar.add(resetBtn);
         }

         panel.add(toolBar, "grow");
      }

      if (showLayerTable) {
         mapLayerTable = new MapLayerTable(mapPane);

         /*
          * We put the map layer panel and the map pane into a JSplitPane
          * so that the user can adjust their relative sizes as needed
          * during a session. The call to setPreferredSize for the layer
          * panel has the effect of setting the initial position of the
          * JSplitPane divider
          */
         mapLayerTable.setPreferredSize(new Dimension(200, -1));
         JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, mapLayerTable, mapPane);
         panel.add(splitPane, "grow");

      }
      else {
         /*
          * No layer table, just the map pane
          */
         panel.add(mapPane, "grow");
      }

      if (showStatusBar) {
         statusBar = new StatusBar(mapPane);
         panel.add(statusBar, "grow");
      }

      this.add(panel);
      uiSet = true;
   }

   /**
    * Get the map context associated with this frame.
    * Returns {@code null} if no map context has been set explicitly with the
    * constructor or {@linkplain #setMapContext}.
    *
    * @return the current {@code MapContext} object
    */
   public MapContext getMapContext() {
      return mapPane.getMapContext();
   }

   /**
    * Set the MapContext object used by this frame.
    *
    * @param context a MapContext instance
    * @throws IllegalArgumentException if context is null
    */
   public void setMapContext(MapContext context) {
      if (context == null) {
         throw new IllegalArgumentException("context must not be null");
      }

      mapPane.setMapContext(context);
   }

   /**
    * Get the renderer being used by this frame.
    * Returns {@code null} if no renderer was set via the constructor
    * or {@linkplain #setRenderer}.
    *
    * @return the current {@code GTRenderer} object
    */
   public GTRenderer getRenderer() {
      return mapPane.getRenderer();
   }

   /**
    * Set the renderer to be used by this frame.
    *
    * @param renderer a GTRenderer instance
    * @throws IllegalArgumentException if renderer is null
    */
   public void setRenderer(GTRenderer renderer) {
      if (renderer == null) {
         throw new IllegalArgumentException("renderer must not be null");
      }

      mapPane.setRenderer(renderer);
   }

   /**
    * Provides access to the instance of {@code JMapPane} being used
    * by this frame.
    *
    * @return the {@code JMapPane} object
    */
   public JMapPane getMapPane() {
      return mapPane;
   }

   /**
    * Provides access to the toolbar being used by this frame.
    * If {@linkplain #initComponents} has not been called yet
    * this method will invoke it.
    *
    * @return the toolbar or null if the toolbar was not enabled
    */
   public JToolBar getToolBar() {
      if (!uiSet) {
         initComponents();
      }
      return toolBar;
   }


   /*
    * Some functions added for drawing maps from GeoTools source (StyleFunctionLab).
    */

   
   /**
    * Create a style to draw maps with
    * @param featureSource The feature source to draw
    * @param columnToMap If null then use a default style, otherwise look for
    * the given column and try to colour
    * based on that field.
    */
   public static Style createDefaultStyle(FeatureSource featureSource, String columnToMap) throws IOException {

      SimpleFeatureType schema = (SimpleFeatureType) featureSource.getSchema();
      Class geomType = schema.getGeometryDescriptor().getType().getBinding();

      if (Polygon.class.isAssignableFrom(geomType)
              || MultiPolygon.class.isAssignableFrom(geomType)) {
         return createPolygonStyle(featureSource, columnToMap);

      }
      else {
         return createPointStyle();
      }

   }

   /**
    * Create a Style to draw polygon features with a thin blue outline and
    * a cyan fill.
    * I have adapted it to colour depending on the value of the results
    * column from the SpatialTest algorithm (if appropriate).
    * See
    * http://docs.geotools.org/latest/userguide/examples/stylefunctionlab.html
    *
    * @param featureSource The feature source to draw
    * @param columnToMap If null then use a default style, otherwise look for
    * the given column and try to colour
    */
   public static Style createPolygonStyle(FeatureSource featureSource, String columnToMap) throws IOException {

      Style style = null;
      Stroke stroke = null;
      Fill fill = null;
      if ( columnToMap==null || columnToMap.length()==0) { // Default colours

//         System.out.println("No column to map specified");

         // create a partially opaque outline stroke
         stroke = styleFactory.createStroke(
                 filterFactory.literal(Color.BLUE),
                 filterFactory.literal(1),
                 filterFactory.literal(0.5));

         // create a partial opaque fill
         fill = styleFactory.createFill(
                 filterFactory.literal(Color.CYAN),
                 filterFactory.literal(0.5));




      }
      else { // Colour based on the value of the given column

//         System.out.println("Column to map is: "+columnToMap);

        ColorLookupFunction colourFn = new ColorLookupFunction(
                featureSource.getFeatures(), filterFactory.property(columnToMap));

        stroke = styleFactory.createStroke(
                colourFn,                      // function to choose feature colour
                filterFactory.literal(1.0f),   // line width
                filterFactory.literal(1.0f));  // opacity

        fill = styleFactory.createFill(
                colourFn,                      // function to choose feature colour
                filterFactory.literal(1.0f));  // opacity

//        System.out.println("FILL IS: "+fill.toString());


      }

      /*
       * Setting the geometryPropertyName arg to null signals that we want to
       * draw the default geomettry of features
       */
      PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke, fill, null);

      Rule rule = styleFactory.createRule();
      rule.symbolizers().add(sym);
      FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
      style = styleFactory.createStyle();
      style.featureTypeStyles().add(fts);
      return style;
   }

   /**
    * Create a Style to draw point features as circles with blue outlines
    * and cyan fill
    */
   public static Style createPointStyle() {
      Graphic gr = styleFactory.createDefaultGraphic();

      Mark mark = styleFactory.getCircleMark();

      mark.setStroke(styleFactory.createStroke(
              filterFactory.literal(Color.BLUE), filterFactory.literal(1)));

      mark.setFill(styleFactory.createFill(filterFactory.literal(Color.CYAN)));

      gr.graphicalSymbols().clear();
      gr.graphicalSymbols().add(mark);
      gr.setSize(filterFactory.literal(5));

      /*
       * Setting the geometryPropertyName arg to null signals that we want to
       * draw the default geomettry of features
       */
      PointSymbolizer sym = styleFactory.createPointSymbolizer(gr, null);

      Rule rule = styleFactory.createRule();
      rule.symbolizers().add(sym);
      FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
      Style style = styleFactory.createStyle();
      style.featureTypeStyles().add(fts);

      return style;
   }

       /**
     * A function to dynamically allocate colours to features. It works with a lookup table
     * where the key is a user-specified feature attribute. Colours are generated using
     * a simple colour ramp algorithm.
     */
    static class ColorLookupFunction extends FunctionExpressionImpl {

        private static final float INITIAL_HUE = 0.1f;
        private final FeatureCollection collection;

        Map<Object, Color> lookup;
        private int numColours;

        private float hue;
        private float hueIncr;
        private float saturation = 0.7f;
        private float brightness = 0.7f;


        /**
         * Creates an instance of the function for the given feature collection. Features will
         * be assigned fill colours by matching the value of the specified feature attribute
         * in a lookup table of unique attribute values with associated colours.
         *
         * @param collection the feature collection
         *
         * @param colourAttribute a literal expression that specifies the feature attribute
         *        to use for colour lookup
         */
        public ColorLookupFunction(FeatureCollection collection, Expression colourAttribute) {
            super("UniqueColour");
            this.collection = collection;

            this.params.add(colourAttribute);
            this.fallback = CommonFactoryFinder.getFilterFactory2(null).literal(Color.WHITE);
        }

        @Override
        public int getArgCount() {
            return 1;
        }

        /**
         * Evalute this function for a given feature and return a
         * Color.
         *
         * @param object the feature for which a colour is being requested
         *
         * @return the colour for this feature
         */
        @Override
        public Object evaluate(Object feature) {
            if (lookup == null) {
                createLookup();
            }

            Object key = ((Expression)params.get(0)).evaluate(feature);
            Color color = lookup.get(key);
            if (color == null) {
                color = addColor(key);
            }

            return color;
        }

        /**
         * Creates the lookup table and initializes variables used in
         * colour generation
         */
        private void createLookup() {
            lookup = new HashMap<Object, Color>();
            try {
                UniqueVisitor visitor = new UniqueVisitor((Expression)params.get(0));
                collection.accepts(visitor, null);
                numColours = visitor.getUnique().size();
                hue = INITIAL_HUE;
                hueIncr = (1.0f - hue) / numColours;

            } catch (Exception ex) {
                throw new IllegalStateException("Problem creating colour lookup", ex);
            }
        }

        /*
         * Generates a new colour for the colour ramp and adds it to
         * the lookup table
         */
        private Color addColor(Object key) {
            Color c = new Color(Color.HSBtoRGB(hue, saturation, brightness));
            hue += hueIncr;
            lookup.put(key, c);
            return c;
        }
    }

}

