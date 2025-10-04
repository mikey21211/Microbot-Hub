package net.runelite.client.plugins.microbot.ScreenRotation;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.depositbox.DepositBoxLocation;
import net.runelite.client.plugins.microbot.util.walker.enums.Allotments;
import net.runelite.client.plugins.microbot.util.walker.enums.Birds;
import net.runelite.client.plugins.microbot.util.walker.enums.Bushes;
import net.runelite.client.plugins.microbot.util.walker.enums.Chinchompas;
import net.runelite.client.plugins.microbot.util.walker.enums.CompostBins;
import net.runelite.client.plugins.microbot.util.walker.enums.Farming;
import net.runelite.client.plugins.microbot.util.walker.enums.FruitTrees;
import net.runelite.client.plugins.microbot.util.walker.enums.Herbs;
import net.runelite.client.plugins.microbot.util.walker.enums.Hops;
import net.runelite.client.plugins.microbot.util.walker.enums.HuntingAreas;
import net.runelite.client.plugins.microbot.util.walker.enums.Insects;
import net.runelite.client.plugins.microbot.util.walker.enums.Kebbits;
import net.runelite.client.plugins.microbot.util.walker.enums.Salamanders;
import net.runelite.client.plugins.microbot.util.walker.enums.SlayerMasters;
import net.runelite.client.plugins.microbot.util.walker.enums.SpecialHuntingAreas;
import net.runelite.client.plugins.microbot.util.walker.enums.Trees;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

public class ScreenRotationPanel extends PluginPanel {

    private final ScreenRotationPlugin plugin;
    private JTextField xField, yField, zField;
    private JComboBox<BankLocation> bankComboBox;
    private JComboBox<DepositBoxLocation> depositBoxComboBox;
    private JComboBox<SlayerMasters> slayerMasterComboBox;
    private JComboBox<Farming> farmingComboBox;
    private JComboBox<Allotments> allotmentsComboBox;
    private JComboBox<Bushes> bushesComboBox;
    private JComboBox<FruitTrees> fruitTreesComboBox;
    private JComboBox<Herbs> herbsComboBox;
    private JComboBox<Hops> hopsComboBox;
    private JComboBox<Trees> treesComboBox;
    private JComboBox<CompostBins> compostBinsComboBox;
    private JComboBox<HuntingAreas> huntingAreasComboBox;
    private JComboBox<Birds> birdsComboBox;
    private JComboBox<Chinchompas> chinchompasComboBox;
    private JComboBox<Insects> insectsComboBox;
    private JComboBox<Kebbits> kebbitsJComboBox;
    private JComboBox<Salamanders> salamandersComboBox;
    private JComboBox<SpecialHuntingAreas> specialHuntingAreasJComboBox;
    private javax.swing.Timer questInfoTimer;
    private javax.swing.Timer clueInfoTimer;

    @com.google.inject.Inject
    private WorldMapPointManager worldMapPointManager;

    @com.google.inject.Inject
    private Client client;

    @Getter
    @com.google.inject.Inject
    private ClientThread clientThread;

    @Inject
    private ScreenRotationPanel(ScreenRotationPlugin plugin) {
        super();
        this.plugin = plugin;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createCustomLocationPanel());
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private JPanel createCustomLocationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(createCenteredTitledBorder("Travel to Custom Location", "/net/runelite/client/plugins/microbot/shortestpath/Map_link_icon.png"));

        JPanel coordinatesPanel = new JPanel(new GridLayout(2, 3, 5, 5));

        JLabel xLabel = new JLabel("X");
        JLabel yLabel = new JLabel("Y");
        JLabel zLabel = new JLabel("Z");
        xLabel.setHorizontalAlignment(SwingConstants.CENTER);
        yLabel.setHorizontalAlignment(SwingConstants.CENTER);
        zLabel.setHorizontalAlignment(SwingConstants.CENTER);

        xField = new JTextField("0", 5);
        yField = new JTextField("0", 5);
        zField = new JTextField("0", 5);

        xField.setHorizontalAlignment(JTextField.CENTER);
        yField.setHorizontalAlignment(JTextField.CENTER);
        zField.setHorizontalAlignment(JTextField.CENTER);

        coordinatesPanel.add(xLabel);
        coordinatesPanel.add(yLabel);
        coordinatesPanel.add(zLabel);
        coordinatesPanel.add(xField);
        coordinatesPanel.add(yField);
        coordinatesPanel.add(zField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        JPanel bottomRow = new JPanel();
        bottomRow.setLayout(new BoxLayout(bottomRow, BoxLayout.X_AXIS));
        JButton setRotationPointButton = new JButton("Set Rotation Point");

        int combinedWidth = 20;
        int buttonHeight = setRotationPointButton.getPreferredSize().height;

        setRotationPointButton.setMaximumSize(new Dimension(combinedWidth, buttonHeight));
        setRotationPointButton.setPreferredSize(new Dimension(combinedWidth, buttonHeight));
        setRotationPointButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        setRotationPointButton.addActionListener(e -> plugin.setNewRotationPoint(getCustomLocation()));
        bottomRow.add(Box.createHorizontalGlue());
        bottomRow.add(setRotationPointButton);
        bottomRow.add(Box.createHorizontalGlue());

        buttonPanel.add(topRow);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(bottomRow);

        panel.add(coordinatesPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(buttonPanel);

        return panel;
    }

    public WorldPoint getCustomLocation()
    {
        try
        {
            int x = Integer.parseInt(xField.getText());
            int y = Integer.parseInt(yField.getText());
            int z = Integer.parseInt(zField.getText());
            return new WorldPoint(x, y, z);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private Border createCenteredTitledBorder(String title, String iconPath)
    {
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), iconPath);
        ImageIcon imageIcon = new ImageIcon(icon);

        JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>", imageIcon, JLabel.CENTER);
        titleLabel.setHorizontalTextPosition(JLabel.RIGHT);
        titleLabel.setVerticalTextPosition(JLabel.CENTER);

        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border lineBorder = BorderFactory.createLineBorder(Color.GRAY);

        return BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        lineBorder,
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                ),
                new TitledBorder(emptyBorder, title, TitledBorder.CENTER, TitledBorder.TOP, null, null)
                {
                    @Override
                    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
                    {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.translate(x + width / 2 - titleLabel.getPreferredSize().width / 2, y);
                        titleLabel.setSize(titleLabel.getPreferredSize());
                        titleLabel.paint(g2d);
                        g2d.dispose();
                    }
                }
        );
    }
}
