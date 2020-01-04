package org.usfirst.frc.team2077.video.core;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.video.interfaces.RenderedView;
import org.usfirst.frc.team2077.video.interfaces.VideoView;

/**
 * A container for one or more {@link RenderedView} components.
 * <p>
 * This is a simpler and lighter weight for combining multiple source renderings
 * into a view for display only. Add more...
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class CompositeView implements VideoView {

    protected final String name_;
    
    protected final Dimension resolution_;
    
    protected final Set<VideoView> views_ = new HashSet<>();

    protected JComponent jComponent_ = null;
    
    protected Map<String,double[]> viewBounds_ = new HashMap<>();
   
    protected Map<JComponent,double[]> componentBounds_ = new HashMap<>();
 
    /**
     * @param name Key for configuration properties.
     */
    public CompositeView(String name) {
        
        name_ = name;
 
        Dimension resolution = null;
        try {
            String[] s = Main.getProperties().getProperty(name_ + ".resolution").split("x");
            resolution = new Dimension(Integer.parseInt(s[0]), Integer.parseInt(s[1]));
        } catch (Exception ex) {
            resolution = new Dimension(1280, 720);
        }
        resolution_ = resolution;
        
        for (int i = 0;; i++) {
            String viewName = Main.getProperties().getProperty(name_ + ".view" + i);
            if (viewName == null) {
                break;
            }
            String[] b = Main.getProperties().getProperty(name_ + "." + viewName + ".bounds", "0,0,1,1").split(",");
            double[] bounds = new double[4];
            for (int j = 0; j < Math.min(b.length, bounds.length); j++) {
                try {
                    bounds[j] = Double.parseDouble(b[j]);
                }
                catch(Exception ex) {
                }
            }
            viewBounds_.put(viewName, bounds);
        }
        if (viewBounds_.isEmpty()) {
            throw new RuntimeException("No views configured.");
        }
    }
    
    @Override
    public String getName() {
        return name_;
    }
 
    @Override
    public Dimension getResolution() {
        return resolution_;
    }
    
    @Override
    public JComponent getJComponent() {
        if (jComponent_ == null) {
            jComponent_ = new JPanel(new LayoutManager() {
                @Override
                public void addLayoutComponent(String name, Component component) {
                }
                @Override
                public void removeLayoutComponent(Component component) {
                }
                @Override
                public Dimension minimumLayoutSize(Container parent) {
                    return new Dimension(0, 0);
                }
                @Override
                public Dimension preferredLayoutSize(Container parent) {
                    return resolution_;
                }
                @Override
                public void layoutContainer(Container parent) {
                    for (JComponent component : componentBounds_.keySet()) {
                        if (component.getParent() != parent) {
                            parent.add(component); // add back any child components that have been re-parented
                        }
                    }
                    Dimension size = parent.getSize();
                    double[] minMax = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};
                    for (double[] bounds : componentBounds_.values()) {
                        minMax[0] = Math.min(minMax[0],  bounds[0]);
                        minMax[1] = Math.min(minMax[1],  bounds[1]);
                        minMax[2] = Math.max(minMax[2],  bounds[0] + bounds[2]);
                        minMax[3] = Math.max(minMax[3],  bounds[1] + bounds[3]);
                    }
                    double[] scale = {size.getWidth()/minMax[2], size.getHeight()/minMax[3]};
                    //scale[0] = scale[1] = Math.min(scale[0], scale[1]); // preserve aspect ratio
                    for (Map.Entry<JComponent, double[]> entry : componentBounds_.entrySet()) {
                        JComponent component = entry.getKey();
                        double[] xywh = entry.getValue();
                        int[] ltrb = {
                                      (int)Math.round(scale[0] * xywh[0]),
                                      (int)Math.round(scale[1] * xywh[1]),
                                      (int)Math.round(scale[0] * (xywh[0]+xywh[2])),
                                      (int)Math.round(scale[1] * (xywh[1]+xywh[3]))
                        };
                        component.setBounds(ltrb[0], ltrb[1], ltrb[2]-ltrb[0], ltrb[3]-ltrb[1]);
                    }
                }
            });
            jComponent_.setOpaque(true);
            jComponent_.setBackground(Color.black);
            jComponent_.setPreferredSize(resolution_);
//            for (VideoView view : getViews()) {
//                JComponent component = view.getJComponent();
//                componentBounds_.put(component, viewBounds_.get( view.getName()));
//            }
            for (String viewName : viewBounds_.keySet()) {
                VideoView view = Main.getView(viewName);
                JComponent component = view.getJComponent();
                componentBounds_.put(component, viewBounds_.get( view.getName()));
            }
        }
        return jComponent_;
    }
    
    @Override
    public Collection<VideoView> getViews() {
        if (views_.isEmpty()) {
            for (String viewName : viewBounds_.keySet()) {
                VideoView view = Main.getView(viewName);
                views_.addAll(view.getViews());
            }
        }
        return views_;
    }
    
    @Override
    public boolean isMapped() {
        return false;
    }

}
