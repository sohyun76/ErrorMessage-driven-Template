package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import app.helpers.Config;
import model.Strings;
import model.JWTInterceptModel;

public class JWTInterceptTab extends JPanel {

	private static final long serialVersionUID = 1L;
	private JWTInterceptModel jwtIM;
	private RSyntaxTextArea jwtArea;
	private String jwtAreaOriginalContent = "none";
	private JRadioButton rdbtnRecalculateSignature;
	private JRadioButton rdbtnRandomKey;
	private JRadioButton rdbtnOriginalSignature;
	private JRadioButton rdbtnChooseSignature;

	private JTextArea jwtKeyArea;
	private JLabel lblSecretKey;
	private JSeparator separator;
	private JRadioButton rdbtnDontModifySignature;
	private JLabel lblProblem;
	private JComboBox<String> noneAttackComboBox;
	private JLabel lblNewLabel;
	private JLabel lblCookieFlags;
	private JLabel lbRegisteredClaims;
	private JCheckBox chkbxCVEAttack;
	private JButton btnCopyPubPrivKeyCVEAttack;

	public JWTInterceptTab(JWTInterceptModel jwtIM) {
		this.jwtIM = jwtIM;
		drawGui();
	}
	
	public void registerActionListeners(ActionListener dontMofiy, ActionListener randomKeyListener, ActionListener originalSignatureListener, ActionListener recalculateSignatureListener, ActionListener chooseSignatureListener, ActionListener algAttackListener, ActionListener cveAttackListener){
		rdbtnDontModifySignature.addActionListener(dontMofiy);
		rdbtnRecalculateSignature.addActionListener(randomKeyListener);
		rdbtnOriginalSignature.addActionListener(originalSignatureListener);
		rdbtnChooseSignature.addActionListener(chooseSignatureListener);
		rdbtnRandomKey.addActionListener(recalculateSignatureListener);
		noneAttackComboBox.addActionListener(algAttackListener);
		chkbxCVEAttack.addActionListener(cveAttackListener);
	}
	
	private void drawGui() {	
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		jwtArea = new RSyntaxTextArea(20,60);
		jwtArea.setMinimumSize(new Dimension(300, 300));
		jwtArea.setColumns(90);
		SyntaxScheme scheme = jwtArea.getSyntaxScheme();
		Style style = new Style();
		style.foreground = new Color(222,133,10);
		scheme.setStyle(Token.LITERAL_STRING_DOUBLE_QUOTE, style);
		jwtArea.revalidate();
		jwtArea.setHighlightCurrentLine(false);
		jwtArea.setCurrentLineHighlightColor(Color.WHITE);
		jwtArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		jwtArea.setEditable(true);
		jwtArea.setPopupMenu(new JPopupMenu()); 
		RTextScrollPane sp = new RTextScrollPane(jwtArea);
		sp.setLineNumbersEnabled(false);
		
		GridBagConstraints gbc_jwtArea = new GridBagConstraints();
		gbc_jwtArea.gridheight = 7;
		gbc_jwtArea.gridwidth = 1;
		gbc_jwtArea.insets = new Insets(0, 0, 5, 5);
		gbc_jwtArea.fill = GridBagConstraints.BOTH;
		gbc_jwtArea.gridx = 1;
		gbc_jwtArea.gridy = 1;
		add(sp, gbc_jwtArea);

		
		rdbtnDontModifySignature = new JRadioButton(Strings.dontModify);
		rdbtnDontModifySignature.setToolTipText(Strings.dontModifyToolTip);
		rdbtnDontModifySignature.setSelected(true);
		rdbtnDontModifySignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_rdbtnDontModifySignature = new GridBagConstraints();
		gbc_rdbtnDontModifySignature.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDontModifySignature.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnDontModifySignature.gridx = 2;
		gbc_rdbtnDontModifySignature.gridy = 1;
		add(rdbtnDontModifySignature, gbc_rdbtnDontModifySignature);
		
		rdbtnRecalculateSignature = new JRadioButton(Strings.recalculateSignature);
		rdbtnRecalculateSignature.setToolTipText(Strings.recalculateSignatureToolTip);
		rdbtnRecalculateSignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_rdbtnRecalculateSignature = new GridBagConstraints();
		gbc_rdbtnRecalculateSignature.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRecalculateSignature.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRecalculateSignature.gridx = 2;
		gbc_rdbtnRecalculateSignature.gridy = 2;
		add(rdbtnRecalculateSignature, gbc_rdbtnRecalculateSignature);
		
		rdbtnOriginalSignature = new JRadioButton(Strings.keepOriginalSignature);
		rdbtnOriginalSignature.setToolTipText(Strings.keepOriginalSignatureToolTip);
		rdbtnOriginalSignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_rdbtnNewRadioButton_1 = new GridBagConstraints();
		gbc_rdbtnNewRadioButton_1.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton_1.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton_1.gridx = 2;
		gbc_rdbtnNewRadioButton_1.gridy = 3;
		add(rdbtnOriginalSignature, gbc_rdbtnNewRadioButton_1);
		
		rdbtnRandomKey = new JRadioButton(Strings.randomKey);
		rdbtnRandomKey.setToolTipText(Strings.randomKeyToolTip);
		rdbtnRandomKey.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_rdbtnNewRadioButton = new GridBagConstraints();
		gbc_rdbtnNewRadioButton.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton.gridx = 2;
		gbc_rdbtnNewRadioButton.gridy = 4;
		add(rdbtnRandomKey, gbc_rdbtnNewRadioButton);
		
		
		rdbtnChooseSignature = new JRadioButton(Strings.chooseSignature);
		rdbtnChooseSignature.setToolTipText(Strings.chooseSignatureToolTip);
		rdbtnChooseSignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_rdbtnNewRadioButton1 = new GridBagConstraints();
		gbc_rdbtnNewRadioButton1.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton1.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton1.gridx = 2;
		gbc_rdbtnNewRadioButton1.gridy = 5;
		add(rdbtnChooseSignature, gbc_rdbtnNewRadioButton1);
		
		ButtonGroup btgrp = new ButtonGroup();
		btgrp.add(rdbtnDontModifySignature);
		btgrp.add(rdbtnOriginalSignature);
		btgrp.add(rdbtnRandomKey);
		btgrp.add(rdbtnRecalculateSignature);
		btgrp.add(rdbtnChooseSignature);

		separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 2;
		gbc_separator.gridy = 5;
		add(separator, gbc_separator);
		
		lblSecretKey = new JLabel(Strings.interceptRecalculationKey);
		GridBagConstraints gbc_lblSecretKey = new GridBagConstraints();
		gbc_lblSecretKey.insets = new Insets(0, 0, 5, 5);
		gbc_lblSecretKey.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblSecretKey.gridx = 2;
		gbc_lblSecretKey.gridy = 6;
		add(lblSecretKey, gbc_lblSecretKey);
		
		jwtKeyArea = new JTextArea("");
		jwtKeyArea.setEnabled(false);

		GridBagConstraints gbc_keyField = new GridBagConstraints();
		gbc_keyField.anchor = GridBagConstraints.NORTH;
		gbc_keyField.insets = new Insets(0, 0, 5, 5);
		gbc_keyField.fill = GridBagConstraints.HORIZONTAL;
		gbc_keyField.gridx = 2;
		gbc_keyField.gridy = 7;
		jwtKeyArea.setRows(5);
		
        JScrollPane jp = new JScrollPane(jwtKeyArea);
		
		add(jp, gbc_keyField);
		jwtKeyArea.setColumns(2);
		jwtKeyArea.setRows(2);
		jwtKeyArea.setLineWrap(false);
		
		lblProblem = new JLabel("");
		GridBagConstraints gbc_lblProblem = new GridBagConstraints();
		gbc_lblProblem.insets = new Insets(0, 0, 5, 5);
		gbc_lblProblem.gridx = 1;
		gbc_lblProblem.gridy = 8;
		add(lblProblem, gbc_lblProblem);
		
		lblNewLabel = new JLabel("Alg None Attack:");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 2;
		gbc_lblNewLabel.gridy = 9;
		add(lblNewLabel, gbc_lblNewLabel);
		
		lblCookieFlags = new JLabel("");
		GridBagConstraints gbc_lblCookieFlag = new GridBagConstraints();
		gbc_lblCookieFlag.insets = new Insets(0, 0, 5, 5);
		gbc_lblCookieFlag.anchor = GridBagConstraints.WEST;
		gbc_lblCookieFlag.gridx = 1;
		gbc_lblCookieFlag.gridy = 10;
		add(lblCookieFlags, gbc_lblCookieFlag);
		
		noneAttackComboBox = new JComboBox<String>();
		GridBagConstraints gbc_noneAttackComboBox = new GridBagConstraints();
		gbc_noneAttackComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_noneAttackComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_noneAttackComboBox.gridx = 2;
		gbc_noneAttackComboBox.gridy = 10;
		add(noneAttackComboBox, gbc_noneAttackComboBox);
		
		chkbxCVEAttack = new JCheckBox("CVE-2018-0114 Attack");
		chkbxCVEAttack.setToolTipText("The public and private key used can be found in src/app/helpers/Strings.java");
		chkbxCVEAttack.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_chkbxCVEAttack = new GridBagConstraints();
		gbc_chkbxCVEAttack.anchor = GridBagConstraints.WEST;
		gbc_chkbxCVEAttack.insets = new Insets(0, 0, 5, 5);
		gbc_chkbxCVEAttack.gridx = 2;
		gbc_chkbxCVEAttack.gridy = 11;
		add(chkbxCVEAttack, gbc_chkbxCVEAttack);
		
		lbRegisteredClaims = new JLabel();
		lbRegisteredClaims.setBackground(SystemColor.controlHighlight);
		GridBagConstraints gbc_lbRegisteredClaims = new GridBagConstraints();
		gbc_lbRegisteredClaims.insets = new Insets(0, 0, 5, 5);
		gbc_lbRegisteredClaims.fill = GridBagConstraints.BOTH;
		gbc_lbRegisteredClaims.gridx = 2;
		gbc_lbRegisteredClaims.gridy = 12;
		add(lbRegisteredClaims, gbc_lbRegisteredClaims);
		
		btnCopyPubPrivKeyCVEAttack = new JButton("Copy used public &private key to clipboard used in CVE attack");
		btnCopyPubPrivKeyCVEAttack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Toolkit.getDefaultToolkit()
		        .getSystemClipboard()
		        .setContents(
		                new StringSelection("Public Key:\r\n"+Config.cveAttackModePublicKey+"\r\n\r\nPrivate Key:\r\n"+Config.cveAttackModePrivateKey),
		                null
		        );
			}
		});
		btnCopyPubPrivKeyCVEAttack.setVisible(false);
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 0, 5);
		gbc_button.gridx = 2;
		gbc_button.gridy = 13;
		add(btnCopyPubPrivKeyCVEAttack, gbc_button);
		
		noneAttackComboBox.addItem("  -");
		noneAttackComboBox.addItem("Alg: none");
		noneAttackComboBox.addItem("Alg: None");
		noneAttackComboBox.addItem("Alg: nOnE");
		noneAttackComboBox.addItem("Alg: NONE");
		
	}
	
	public AbstractButton getRdbtnDontModify() {
		return rdbtnDontModifySignature;
	}
	
	public JRadioButton getRdbtnChooseSignature() {
		return rdbtnChooseSignature;
	}
	
	public JRadioButton getRdbtnRecalculateSignature() {
		return rdbtnRecalculateSignature;
	}
	
	public JComboBox<String> getNoneAttackComboBox() {
		return noneAttackComboBox;
	}
	
	public JCheckBox getCVEAttackCheckBox() {
		return chkbxCVEAttack;
	}

	public JRadioButton getRdbtnRandomKey() {
		return rdbtnRandomKey;
	}

	public JButton getCVECopyBtn(){
		return btnCopyPubPrivKeyCVEAttack;
	}
	
	public JRadioButton getRdbtnOriginalSignature() {
		return rdbtnOriginalSignature;
	}

	public void updateSetView(final boolean reset) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(!jwtArea.getText().equals(jwtIM.getJWTJSON())){
					jwtArea.setText(jwtIM.getJWTJSON());
					jwtAreaOriginalContent = jwtIM.getJWTJSON();
				}
				jwtKeyArea.setText(jwtIM.getJWTKey());
				if(reset){
					rdbtnDontModifySignature.setSelected(true);
					jwtKeyArea.setText("");
					jwtKeyArea.setEnabled(false);
				}
				jwtArea.setCaretPosition(0);
				lblProblem.setText(jwtIM.getProblemDetail());
				
				if(jwtIM.getcFW().isCookie()){
					lblCookieFlags.setText(jwtIM.getcFW().toHTMLString());
				}else{
					lblCookieFlags.setText("");
				}
				lbRegisteredClaims.setText(jwtIM.getTimeClaimsAsText());
			}
		});
	}
	
	public JTextArea getJwtArea() {
		return jwtArea;
	}
	
	public  RSyntaxTextArea getJwtAreaAsRSyntax() {
		return jwtArea;
	}
	
	public void setKeyFieldState(boolean state){
		jwtKeyArea.setEnabled(state);
	}
	
	public boolean jwtWasChanged() {
		if(jwtArea.getText()==null) {
			return false;
		}
		return !jwtAreaOriginalContent.equals(jwtArea.getText());
	}
	
	public String getJWTfromArea(){
		return jwtArea.getText();
	}
	
	public String getSelectedData() {
		return jwtArea.getSelectedText();
	}

	public String getKeyFieldValue() {
		return jwtKeyArea.getText();
	}
	
	public void setKeyFieldValue(String string) {
		jwtKeyArea.setText(string);
	}
}