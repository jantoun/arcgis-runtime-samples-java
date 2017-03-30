/*
 * Copyright 2016 Esri.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.esri.samples.localserver.local_server_dynamic_workspace_raster;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.layers.ArcGISMapImageLayer;
import com.esri.arcgisruntime.layers.ArcGISMapImageSublayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.localserver.LocalMapService;
import com.esri.arcgisruntime.localserver.LocalServer;
import com.esri.arcgisruntime.localserver.LocalServer.StatusChangedEvent;
import com.esri.arcgisruntime.localserver.LocalServerStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;

public class LocalServerDynamicWorkspaceRaster extends Application {

  private static final int APPLICATION_WIDTH = 800;

  private ArcGISMap map;
  private MapView mapView;
  private LocalMapService mapImageService;
  private ProgressIndicator imageLayerProgress;

  private static final LocalServer server = LocalServer.INSTANCE;

  @Override
  public void start(Stage stage) throws Exception {

    try {
      // create stack pane and application scene
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);

      // set title, size, and add scene to stage
      stage.setTitle("Local Server Dynamic Workspace Raster");
      stage.setWidth(APPLICATION_WIDTH);
      stage.setHeight(700);
      stage.setScene(scene);
      stage.show();

      // create a view with a map and basemap
      map = new ArcGISMap(Basemap.createStreets());
      mapView = new MapView();
      mapView.setMap(map);

      // track progress of loading feature layer to map
      imageLayerProgress = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
      imageLayerProgress.setMaxWidth(30);

      // start local server
      server.startAsync();
      server.addStatusChangedListener(status -> {
        if (server.getStatus() == LocalServerStatus.STARTED) {
          // start feature service
          //            String featureServiceURL = new File("samples-data/local_server/PointsofInterest.mpk").getAbsolutePath();
          String mapServiceURL = "/Program Files (x86)/ArcGIS SDKs/java10.2.4/sdk/samples/data/mpks/mpk_blank.mpk";
          mapImageService = new LocalMapService(mapServiceURL);
          mapImageService.addStatusChangedListener(this::addLocalMapImageLayer);
          mapImageService.startAsync();
        }
      });

      // add view to application window
      stackPane.getChildren().addAll(mapView, imageLayerProgress);
      StackPane.setAlignment(imageLayerProgress, Pos.CENTER);
    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  /**
   * Once the map service starts, a map image layer is created from that service and added to the map.
   * <p>
   * When the map image layer is done loading, the view will zoom to the location of were the image has been added.
   * 
   * @param status status of feature service
   */
  private void addLocalMapImageLayer(StatusChangedEvent status) {

    // check that the map service has started
    if (status.getNewStatus() == LocalServerStatus.STARTED) {
      // get the url of where map service is located
      String url = mapImageService.getUrl();
      // create a map image layer using url
      ArcGISMapImageLayer imageLayer = new ArcGISMapImageLayer(url);
      // set viewpoint once layer has loaded
      imageLayer.addDoneLoadingListener(() -> {
        if (imageLayer.getLoadStatus() == LoadStatus.LOADED && imageLayer.getFullExtent() != null) {
          mapView.setViewpoint(new Viewpoint(imageLayer.getFullExtent()));
          Platform.runLater(() -> imageLayerProgress.setVisible(false));

          RasterWorkspaceSource tableSource = new RasterWorkspaceSource("raster_fgdb", "DynamicWorkspace_Raster");
          DataSublayerSource source = new DataSublayerSource(tableSource);
          ArcGISMapImageSublayer newSL = new ArcGISMapImageSublayer(101, source);
          imageLayer.getSublayers().add(newSL);
        }
      });
      imageLayer.loadAsync();
      // add image layer to map
      mapView.getMap().getOperationalLayers().add(imageLayer);
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() throws Exception {
    if (mapView != null) {
      mapView.dispose();
    }
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }
}
