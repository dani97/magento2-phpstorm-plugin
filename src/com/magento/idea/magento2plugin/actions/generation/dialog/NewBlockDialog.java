/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.generation.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.magento.idea.magento2plugin.actions.generation.NewBlockAction;
import com.magento.idea.magento2plugin.actions.generation.data.BlockFileData;
import com.magento.idea.magento2plugin.actions.generation.dialog.validator.NewBlockValidator;
import com.magento.idea.magento2plugin.actions.generation.generator.ModuleBlockClassGenerator;
import com.magento.idea.magento2plugin.magento.files.BlockPhp;
import com.magento.idea.magento2plugin.magento.packages.File;
import com.magento.idea.magento2plugin.magento.packages.Package;
import com.magento.idea.magento2plugin.util.magento.GetModuleNameByDirectoryUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

public class NewBlockDialog extends AbstractDialog {
    private final NewBlockValidator validator;
    private final PsiDirectory baseDir;
    private final String moduleName;
    private JPanel contentPanel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField blockName;
    private JTextField blockParentDir;
    private final Project project;
    private JTextPane warning;//NOPMD
    private JRadioButton adminhtmlRadioButton;//NOPMD

    /**
     * Constructor.
     *
     * @param project Project
     * @param directory PsiDirectory
     */
    public NewBlockDialog(final Project project, final PsiDirectory directory) {
        super();

        this.project = project;
        this.baseDir = directory;
        this.moduleName = GetModuleNameByDirectoryUtil.execute(directory, project);
        this.validator = NewBlockValidator.getInstance(this);

        setContentPane(contentPanel);
        setModal(true);
        setTitle("Create a new Magento 2 block..");
        getRootPane().setDefaultButton(buttonOK);
        suggestBlockDirectory();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent event) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    /**
     * Open dialog.
     *
     * @param project Project
     * @param directory PsiDirectory
     */
    public static void open(final Project project, final PsiDirectory directory) {
        final NewBlockDialog dialog = new NewBlockDialog(project, directory);
        dialog.pack();
        dialog.centerDialog(dialog);
        dialog.setVisible(true);
    }

    protected void onOK() {
        if (!validator.validate()) {
            return;
        }
        generateFile();
        this.setVisible(false);
    }

    private PsiFile generateFile() {
        return new ModuleBlockClassGenerator(new BlockFileData(
                getBlockDirectory(),
                getBlockName(),
                getModuleName(),
                getNamespace()
        ), project).generate(NewBlockAction.ACTION_NAME, true);
    }

    private String getModuleName() {
        return moduleName;
    }

    public String getBlockName() {
        return blockName.getText().trim();
    }

    public String getBlockDirectory() {
        return blockParentDir.getText().trim();
    }

    private void suggestBlockDirectory() {
        final String moduleIdentifierPath = getModuleIdentifierPath();
        if (moduleIdentifierPath == null) {
            blockParentDir.setText(BlockPhp.DEFAULT_DIR);
            return;
        }
        final String path = baseDir.getVirtualFile().getPath();
        final String[] pathParts = path.split(moduleIdentifierPath);
        final int minimumPathParts = 2;
        if (pathParts.length != minimumPathParts) {
            blockParentDir.setText(BlockPhp.DEFAULT_DIR);
            return;
        }

        if (pathParts[1] != null) {
            blockParentDir.setText(pathParts[1].substring(1));
            return;
        }
        blockParentDir.setText(BlockPhp.DEFAULT_DIR);
    }

    private String getModuleIdentifierPath() {
        final String[]parts = moduleName.split(Package.vendorModuleNameSeparator);
        if (parts[0] == null || parts[1] == null || parts.length > 2) {
            return null;
        }
        return parts[0] + File.separator + parts[1];
    }

    private String getNamespace() {
        final String[]parts = moduleName.split(Package.vendorModuleNameSeparator);
        if (parts[0] == null || parts[1] == null || parts.length > 2) {
            return null;
        }
        final String directoryPart = getBlockDirectory().replace(
                File.separator,
                Package.fqnSeparator
        );
        return parts[0] + Package.fqnSeparator + parts[1] + Package.fqnSeparator + directoryPart;
    }

    public void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
