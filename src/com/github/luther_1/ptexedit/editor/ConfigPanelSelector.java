package com.github.luther_1.ptexedit.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.github.luther_1.ptexedit.papafile.PapaComponent;
import com.github.luther_1.ptexedit.papafile.PapaFile;
import com.github.luther_1.ptexedit.papafile.PapaTexture;

public class ConfigPanelSelector extends JPanel {
	
	private static final long serialVersionUID = 2925995949876826577L;
	private final DefaultMutableTreeNode root;
	private DefaultMutableTreeNode lastSelected;
	public final JTree fileTree;
	private final JScrollPane treeScrollPane;
	private final DefaultTreeModel treeModel;
	private boolean alwaysShowRoot;
	private HashSet<TreePath> expandedPaths = new HashSet<TreePath>();
	private JMenuItem removeSelected, addLinked;
	
	private Editor editor;
	
	public ConfigPanelSelector(Editor editor) {
		this.editor = editor;

		setBorder(BorderFactory.createLineBorder(new Color(192, 192, 192)));
		
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		
		root = new DefaultMutableTreeNode("Open Files");
		fileTree = new JTree(root);
		treeModel = ((DefaultTreeModel)fileTree.getModel());
		
		fileTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TreePath newPath = e.getNewLeadSelectionPath();
				
				if(newPath==null) // This happened once and I have no idea why.
					return;
				
				lastSelected = (DefaultMutableTreeNode)newPath.getLastPathComponent();
				Object o = lastSelected.getUserObject();
				Class<?> cl = o.getClass();
				if(o instanceof PapaComponent) {
					PapaComponent pc = (PapaComponent)o;
					editor.dependents.clear();
					editor.dependents.addAll(Arrays.asList(pc.getDependents()));
					editor.dependencies.clear();
					editor.dependencies.addAll(Arrays.asList(pc.getDependencies()));
					editor.configSelector.fileTree.repaint();
				}
				if(cl == PapaFile.class) {
					PapaFile selected = (PapaFile) o;
					editor.setActiveFile(selected);
				} else if(cl == PapaTexture.class) {
					PapaTexture selected = (PapaTexture) o;
					editor.setActiveTexture(selected);
				} else {
					editor.unloadFileFromConfig();
				}
			}
		});
		
		fileTree.addTreeExpansionListener(new TreeExpansionListener() {
			
			@Override
			public void treeExpanded(TreeExpansionEvent e) {
				expandedPaths.add(e.getPath());
			}
			
			@Override
			public void treeCollapsed(TreeExpansionEvent e) {
				expandedPaths.remove(e.getPath());
			}
		});
		
		initializeTransferHandler();
		fileTree.setCellRenderer(papaTreeRenderer);
		fileTree.setDragEnabled(true);
		fileTree.setDropMode(DropMode.ON);
		fileTree.setRootVisible(true);
		
		treeScrollPane = new JScrollPane(fileTree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeScrollPane.setBorder(BorderFactory.createLineBorder(new Color(192, 192, 192)));
		layout.putConstraint(SpringLayout.NORTH, treeScrollPane, 5, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, treeScrollPane, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, treeScrollPane, -5, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, treeScrollPane, -25, SpringLayout.SOUTH, this);
		add(treeScrollPane);
		
		final JPopupMenu popup = new JPopupMenu();
        removeSelected = new JMenuItem("Remove Selected");
        removeSelected.setMnemonic(KeyEvent.VK_R);
        removeSelected.addActionListener((ActionEvent e)-> {
        	HashSet<Pair<DefaultMutableTreeNode,PapaFile>> nodes = new HashSet<>();
        	TreePath[] selected = fileTree.getSelectionPaths();
			fileTree.clearSelection();
			if(selected==null)
				return;
			editor.unloadFileFromConfig();
			for(TreePath t : selected) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)t.getLastPathComponent();
				Object o = node.getUserObject();
				
				DefaultMutableTreeNode refreshNode = getLowestUnlinkedNode((DefaultMutableTreeNode) t.getLastPathComponent());
				if(refreshNode==null) // was already removed
					continue;
				PapaFile p = getAssociatedPapaFile(refreshNode);
				if(p==null)
					continue;
				
				if(o.getClass()==PapaTexture.class) {
					PapaTexture t2 = (PapaTexture)o;
					if(t2.getParent().isLinkedFile())
						t2.getParent().detach();
					t2.detach();
				}
				if(o.getClass()==PapaFile.class) {
					PapaFile p2 = (PapaFile)o;
					if(p2.isLinkedFile())
						p2.detach();
					else
						removeFromTreeHelper(node);
				}
				nodes.add(new Pair<>(refreshNode,p));
			}
			for(Pair<DefaultMutableTreeNode,PapaFile> p : nodes) {
				if(p.getValue().getNumTextures()== 0 && !editor.ALLOW_EMPTY_FILES)
					removeFromTreeHelper(p.getKey());
				else
					refreshEntry(p.getKey(), p.getValue());
			}
			reloadAndExpand(root);
        });
        popup.add(removeSelected);
        removeSelected.setEnabled(false);
        
        addLinked = new JMenuItem("Add Linked Texture");
        addLinked.setMnemonic(KeyEvent.VK_A);
        addLinked.addActionListener((ActionEvent e)-> {
        	TreePath path = fileTree.getSelectionPath();
        	if(path==null)
        		return;
        	fileTree.clearSelection();
        	DefaultMutableTreeNode refreshNode = getLowestUnlinkedNode((DefaultMutableTreeNode) path.getLastPathComponent());
        	PapaFile p = getAssociatedPapaFile((DefaultMutableTreeNode) path.getLastPathComponent());
        	PapaTexture t = new PapaTexture("New Linked File", null);
        	t.attach(p);
        	
        	DefaultMutableTreeNode replaced = refreshEntry(refreshNode);
        	reloadAndExpand(root);
        	
        	DefaultMutableTreeNode toSelect = replaced;
        	@SuppressWarnings("unchecked")
			//Enumeration<DefaultMutableTreeNode> en = replaced.children();
        	Enumeration<TreeNode> en = replaced.children();
        	while(en.hasMoreElements()) {
        		DefaultMutableTreeNode n = (DefaultMutableTreeNode)en.nextElement();
        		if(n.getUserObject()==t)
        			toSelect = n;
        	}
        	select(new TreePath(toSelect.getPath()));
        	
        	
        });
        popup.add(addLinked);
        addLinked.setEnabled(false);
		
		JButton unloadButton = new JButton("Unload");
		layout.putConstraint(SpringLayout.NORTH, unloadButton, 5, SpringLayout.SOUTH, treeScrollPane);
		layout.putConstraint(SpringLayout.WEST, unloadButton, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, unloadButton, -24, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, unloadButton, -5, SpringLayout.SOUTH, this);
		add(unloadButton);
		
		JButton popupButton = new JButton(editor.upArrowIcon);
		popupButton.setMargin(new Insets(0, 0, 0, 0));
		layout.putConstraint(SpringLayout.NORTH, popupButton, 5, SpringLayout.SOUTH, treeScrollPane);
		layout.putConstraint(SpringLayout.WEST, popupButton, 5, SpringLayout.EAST, unloadButton);
		layout.putConstraint(SpringLayout.EAST, popupButton, -5, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, popupButton, -5, SpringLayout.SOUTH, this);
		add(popupButton);
		popupButton.setFocusPainted(false);
		
        popupButton.addActionListener((ActionEvent e) -> {
        	popup.show(popupButton, 0, (int)-popup.getPreferredSize().getHeight());
		});
        
        unloadButton.addActionListener((ActionEvent e) -> {
			TreePath[] selected = fileTree.getSelectionPaths();
			fileTree.clearSelection();
			if(selected==null)
				return;
			editor.unloadFileFromConfig();
			for(TreePath t : selected) {
				DefaultMutableTreeNode node = getLowestUnlinkedNode((DefaultMutableTreeNode) t.getLastPathComponent());
				if(node==null) // was already removed
					continue;
				PapaFile p = getAssociatedPapaFile(node);
				if(p==null)
					continue;
				
				removeFromTreeHelper(node);
				p.flush();
			}
			reloadAndExpand(root);
		});
	}
	
	public DefaultMutableTreeNode refreshEntry(DefaultMutableTreeNode node) {
		PapaFile p = getAssociatedPapaFile(node);
		if(p==null)
			return node;
		return refreshEntry(node, p);
	}
	
	public void unload() {
		removeSelected.setEnabled(false);
		addLinked.setEnabled(false);
		
	}

	public void applySettings(PapaFile pf, int image, PapaTexture tex, boolean changed) {
		removeSelected.setEnabled(true);
		addLinked.setEnabled(!pf.isLinkedFile());
	}

	public DefaultMutableTreeNode refreshEntry(DefaultMutableTreeNode node, PapaFile p) {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		if(parent == null)
			parent = root; 
		if(p==null)
			return node;
		
		if(p.getNumTextures()!=0 || editor.ALLOW_EMPTY_FILES) {
			removeChildrenFromNode(node);
			DefaultMutableTreeNode first = generateHierarchy(p);
			for(DefaultMutableTreeNode n : getChildren(first))
				node.add(n);
			node.setUserObject(first.getUserObject());
		}
		reloadAndExpand(parent);
		select(new TreePath(node.getPath()));
		return node;
	}

	private void reloadAndExpand(DefaultMutableTreeNode node) {
		TreePath[] selected = fileTree.getSelectionPaths();
		TreePath mainSelect = fileTree.getSelectionPath();
		ArrayList<TreePath> selectedArray = new ArrayList<TreePath>();
		if(selected!=null)
			for(TreePath t : selected)
				selectedArray.add(t);
		if(mainSelect!=null) {
			selectedArray.remove(mainSelect);
			selectedArray.add(0,mainSelect);
		}
		
		@SuppressWarnings("unchecked")
		HashSet<TreePath> expanded = (HashSet<TreePath>) expandedPaths.clone();
		
		treeModel.reload(node);
		
		// Expanding paths
		for(TreePath t : expanded) {
			fileTree.expandPath(t);
		}
		
		removeIfAncestor(root, expanded); // find all paths which do not exist anymore
		for(TreePath t : expanded) {
			TreePath similar = findSimilar(t);
			if(similar != null) {
				fileTree.expandPath(similar);
			}
		}
		
		expandedPaths.removeAll(expanded); // remove non existent paths
		
		ArrayList<TreePath> toSelect = new ArrayList<TreePath>();
		for(TreePath t : selectedArray) {
			TreePath similar = findSimilar(t);
			if(similar != null)
				toSelect.add(similar);
		}
		fileTree.setSelectionPaths(toSelect.toArray(new TreePath[toSelect.size()]));
	}

	private void removeIfAncestor(DefaultMutableTreeNode node, Collection<TreePath> c) {
		for(Iterator<TreePath> treeIterator = c.iterator();treeIterator.hasNext();) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) treeIterator.next().getLastPathComponent();
			if(n.isNodeAncestor(node))
				treeIterator.remove();
		}
	}

	private TreePath findSimilar(TreePath t) { // expensive operation
		DefaultMutableTreeNode n = (DefaultMutableTreeNode) t.getLastPathComponent();
		if(n.isNodeAncestor(root))
			return t;
		
		TreeNode[] newPath = new TreeNode[t.getPathCount()];
		newPath[0] = root;
		
		DefaultMutableTreeNode current = root;
		for(int i = 1;i<t.getPathCount();i++) {
			DefaultMutableTreeNode test = (DefaultMutableTreeNode) t.getPathComponent(i);
			Object testComp = test.getUserObject();
			
			DefaultMutableTreeNode child = getSimilarChildFromObject(current, testComp);
			if(child==null)
				return null;
			
			newPath[i] = child;
			current = child;
		}
		return new TreePath(newPath);
	}
	
	private DefaultMutableTreeNode getSimilarChildFromObject(DefaultMutableTreeNode host, Object o) {
		for(Enumeration<TreeNode> e = host.children();e.hasMoreElements();) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
			Object childComp = child.getUserObject();
			if(o.getClass()==String.class ? o.equals(childComp) : o == childComp)
				return child;
		}
		return null;
	}

	public void setAlwaysShowRoot(boolean selected) {
		this.alwaysShowRoot = selected;
		reloadAll();
	}
	
	public void removeEmptyFiles() {
		SwingUtilities.invokeLater(() -> {
			boolean changed = false;
			for(DefaultMutableTreeNode n : getTopLevelNodes())
				if(n.getUserObject().getClass()==PapaFile.class && ((PapaFile) n.getUserObject()).getNumTextures()==0) {
					changed = true;
					root.remove(n);
				}
			if(changed)
				reloadAndExpand(root);
		});
	}
	
	public void reloadAll() {
		PapaFile selected = lastSelected==null ? null : getAssociatedPapaFile(lastSelected);
		DefaultMutableTreeNode[] nodes = getTopLevelNodes();
		PapaFile[] papafiles = new PapaFile[nodes.length];
		for(int i = 0;i<nodes.length;i++)
			papafiles[i] = getAssociatedPapaFile(nodes[i]);
		
		SwingUtilities.invokeLater(() -> {
			DefaultMutableTreeNode toSelect = null;
			root.removeAllChildren();
			
			for(PapaFile p : papafiles) {
				DefaultMutableTreeNode n = addToTree(p, false);
				if(p == selected)
					toSelect = n;
			}
			
			reloadAndExpand(root);
			if(toSelect!=null) {
				TreePath path = new TreePath(toSelect.getPath());
				select(path);
			}
		});
	}

	private DefaultMutableTreeNode[] getTopLevelNodes() {
		ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
		@SuppressWarnings("unchecked")
		Enumeration<TreeNode> e = root.children();
		while(e.hasMoreElements())
			nodes.add((DefaultMutableTreeNode)e.nextElement());
		return nodes.toArray(new DefaultMutableTreeNode[nodes.size()]);
	}

	public void selectedNodeChanged() {
		if(lastSelected==null)
			return;
		treeModel.nodeChanged(lastSelected);
	}
	
	public void selectedPapaFileNodeChanged() {
		if(lastSelected==null)
			return;
		DefaultMutableTreeNode node = lastSelected;
		while(node!=null && node.getUserObject()!=editor.activeFile)
			node = (DefaultMutableTreeNode) node.getParent();
		if(node!=null)
			treeModel.nodeChanged(node);
		else
			repaint(); // default if we miss
	}
	
	public void reloadTopLevelSelectedNode() {
		if(lastSelected==null)
			return;
		DefaultMutableTreeNode node = getLowestUnlinkedNode(lastSelected);
		if(node==null)
			node = getTopLevelNodeFromPapaFile(editor.activeFile);
		refreshEntry(node);
	}

	public void removeFromTree(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		removeFromTreeHelper(node);
		reloadAndExpand(parent);
	}
	
	private void removeChildrenFromNode(DefaultMutableTreeNode node) {
		for(DefaultMutableTreeNode n : getChildren(node))
			removeFromTreeHelper(n);
	}
	
	private DefaultMutableTreeNode[] getChildren(DefaultMutableTreeNode node) {
		ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
		@SuppressWarnings("unchecked")
		Enumeration<TreeNode> e = node.children();
		while(e.hasMoreElements()) {
			nodes.add((DefaultMutableTreeNode)e.nextElement());
		}
		return nodes.toArray(new DefaultMutableTreeNode[nodes.size()]);
	}

	private void removeFromTreeHelper(DefaultMutableTreeNode node) {
		for(DefaultMutableTreeNode n : getChildren(node))
			removeFromTreeHelper(n);
		node.removeFromParent();
	}
	
	private DefaultMutableTreeNode getLowestUnlinkedNode(DefaultMutableTreeNode node) {
		if(node==null || node.getParent()==null)
			return null;
		Object o = node.getUserObject();
		if(o instanceof String)
			return getLowestUnlinkedNode((DefaultMutableTreeNode)(node.getParent()));
		
		if(o instanceof PapaTexture) { // a texture could either be a top level node, or texture in a file.
			DefaultMutableTreeNode result = getLowestUnlinkedNode((DefaultMutableTreeNode)(node.getParent()));
			if(((PapaTexture)o).isLinked())
				return result;
			if(result==null)
				return node;
			else
				return result;
		}
		
		PapaFile p = getAssociatedPapaFile(node);
		if(p==null)
			return null;
		if(p.isLinkedFile())
			return getLowestUnlinkedNode((DefaultMutableTreeNode)(node.getParent()));
		return node;
	}
	
	private PapaFile getAssociatedPapaFile(DefaultMutableTreeNode node) {
		Object o = node.getUserObject();
		if(o instanceof String)
			return null;
		
		PapaFile p = null;
		if(o instanceof PapaTexture)
			p = ((PapaTexture)o).getParent();
		else
			p = ((PapaFile)o);
		return p;
	}
	
	private DefaultTreeCellRenderer papaTreeRenderer = new DefaultTreeCellRenderer() {
		private static final long serialVersionUID = -3988740979113661683L;
		private final Font font = this.getFont().deriveFont(Font.PLAIN);
		private final Font underline;
		
		{
			Map<TextAttribute, Object> map =new Hashtable<TextAttribute, Object>();
			map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
			underline = font.deriveFont(map);
		}
		
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object o = node.getUserObject();
			
			if(editor.dependencies.contains(o) || editor.dependents.contains(o)) {
				this.setFont(underline);
			} else
				this.setFont(font);
			
			
			if(o instanceof PapaFile) {
				PapaFile p = (PapaFile) o;
				this.setText(p.getFileName());
				this.setIcon(createImageIconFor(p));
			} else if (o instanceof PapaTexture) {
				PapaTexture t = (PapaTexture) o;
				this.setIcon(createImageIconFor(t));
				this.setText(t.getName());
			} else {
				this.setText(o.toString());
			}
			return this;
		}
		
		private ImageIcon createImageIconFor(PapaFile p) {
			ArrayList<ImageIcon> iconList = new ArrayList<ImageIcon>();
			if(!p.getFile().exists())
				iconList.add(editor.imgPapaFileUnsaved);
			if(p.isLinkedFile())
				iconList.add(editor.imgPapafileLinked);
			return createImageIcon((ImageIcon[]) iconList.toArray(new ImageIcon[iconList.size()]));
		}
		
		private ImageIcon createImageIconFor(PapaTexture t) {
			PapaFile p = t.getParent();
			if(p==null) // TODO band aid fix for one frame reload bug
				return createImageIcon();
			
			ArrayList<ImageIcon> iconList = new ArrayList<ImageIcon>();
			
			if(p.getFile() == null || !p.getFile().exists())
				iconList.add(editor.imgPapaFileUnsaved);
			
			iconList.add(editor.imgPapafileImage);
			if(t.isLinked()) {
				iconList.add(editor.imgPapafileLinked);
				if(!t.linkValid())
					iconList.add(editor.imgPapafileError);
			}
			if(p.isLinkedFile() && !p.getParent().isLinkedFileReferenced(p))
				iconList.add(editor.imgPapafileNoLinks);
			return createImageIcon((ImageIcon[]) iconList.toArray(new ImageIcon[iconList.size()]));
				
		}
		
		private ImageIcon createImageIcon(ImageIcon... composite) {
			BufferedImage iconCache = new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
			Graphics iconGraphics = iconCache.getGraphics();
			editor.imgPapafile.paintIcon(this, iconGraphics, 0, 0);
			for(ImageIcon i : composite) {
				i.paintIcon(this, iconGraphics, 0, 0);
			}
			return new ImageIcon(iconCache);
		}
		
	};
	
	/*
	private DefaultMutableTreeNode getNodeFromPapaFile(PapaFile p, DefaultMutableTreeNode n) {
		if(n.getUserObject() instanceof PapaTexture && ((PapaTexture)n.getUserObject()).getParent().equals(p)
			|| n.getUserObject() instanceof PapaFile && ((PapaFile)n.getUserObject()).equals(p)) {
			fileTree.setSelectionPath(new TreePath(n.getPath()));
			requestFocus();
			return n;
		}
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> e = (Enumeration<DefaultMutableTreeNode>)n.children();
		while(e.hasMoreElements()) {
			DefaultMutableTreeNode element = e.nextElement();
			DefaultMutableTreeNode result = getNodeFromPapaFile(p,element);
			if(result!=null)
				return result;
		}
		return null;
	}*/
	
	private DefaultMutableTreeNode getTopLevelNodeFromPapaFile(PapaFile p) {
		Enumeration<TreeNode> e = root.children();
		while(e.hasMoreElements()) {
			DefaultMutableTreeNode element = (DefaultMutableTreeNode)e.nextElement();
			if(element.getUserObject() instanceof PapaTexture && ((PapaTexture)element.getUserObject()).getParent().equals(p)
					|| element.getUserObject() instanceof PapaFile && ((PapaFile)element.getUserObject()).equals(p)) {
				fileTree.setSelectionPath(new TreePath(element.getPath()));
				requestFocus();
				return element;
			}
		}
		return null;
	}
	
	public DefaultMutableTreeNode addToTreeOrSelect(PapaFile p, boolean reload) {
		DefaultMutableTreeNode result = getTopLevelNodeFromPapaFile(p);
		if(result!=null) {
			requestFocus();
			refreshEntry(result); // refresh to account for Link
			fileTree.expandPath(new TreePath(result.getPath()));
			return result;
		}
		return addToTree(p, reload);
	}
	
	public DefaultMutableTreeNode addToTree(PapaFile p, boolean reload) {
		DefaultMutableTreeNode papaRoot = addPapaFileToNode(p,root);
		if(reload) {
			TreePath selected = new TreePath(papaRoot.getPath());
			reloadAndExpand(root);
			select(selected);
		}
		return papaRoot;
	}
	
	private void select(TreePath toSelect) {
		fileTree.setSelectionPath(toSelect);
		fileTree.scrollPathToVisible(toSelect); 
		treeScrollPane.getHorizontalScrollBar().setValue(0); // ensure that the left of the tree is always visible
	}
	
	private DefaultMutableTreeNode addPapaFileToNode(PapaFile file, DefaultMutableTreeNode parent) { // potential crash if two files are linked to each other...
		DefaultMutableTreeNode papaRoot = generateHierarchy(file);
		parent.add(papaRoot);
		return papaRoot;
	}
	
	private DefaultMutableTreeNode generateHierarchy(PapaFile file) {
		DefaultMutableTreeNode papaRoot = new DefaultMutableTreeNode(file);
		
		if(file.containsLinkedFiles()) {
			// add linked files
			DefaultMutableTreeNode linked = new DefaultMutableTreeNode("Linked Files");
			papaRoot.add(linked);
			
			for(PapaFile l : file.getLinkedFiles())
				addPapaFileToNode(l,linked);
			
			for(int i =0;i<file.getNumTextures();i++)
				papaRoot.add(new DefaultMutableTreeNode(file.getTexture(i)));
			
			papaRoot.add(linked);
		} else {
			if( ! alwaysShowRoot && !file.containsComponents(~PapaFile.TEXTURE & ~PapaFile.STRING) && file.getNumTextures()==1 && ! file.getTexture(0).isLinked())
				papaRoot.setUserObject(file.getTexture(0));
			else
				for(int i =0;i<file.getNumTextures();i++)
					papaRoot.add(new DefaultMutableTreeNode(file.getTexture(i)));
		}
		return papaRoot;
	}
	
	private DefaultMutableTreeNode[] getSelectedNodes() {
    	TreePath[] selectedPaths = fileTree.getSelectionPaths();
    	if(selectedPaths == null)
    		return null;
        DefaultMutableTreeNode[] selectedNodes = new DefaultMutableTreeNode[selectedPaths.length];
        for(int i =0;i<selectedPaths.length;i++)
        	selectedNodes[i] = (DefaultMutableTreeNode) selectedPaths[i].getLastPathComponent();
        return selectedNodes;
    }
	
	public void initializeTransferHandler() {
		fileTree.setTransferHandler(new TreeTransferHandler());
	}
	
	public PapaFile[] getTargetablePapaFiles() {
		Enumeration<TreeNode> e =  root.children();
		ArrayList<PapaFile> files = new ArrayList<PapaFile>();
		while(e.hasMoreElements())
			files.add(getAssociatedPapaFile((DefaultMutableTreeNode)e.nextElement()));
		return files.toArray(new PapaFile[files.size()]);
	}
	
	// https://stackoverflow.com/questions/4588109/drag-and-drop-nodes-in-jtree
	private class TreeTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1609783140208717380L;

		private DataFlavor nodesFlavor;
	 	private DataFlavor[] flavors = new DataFlavor[1];
	 	//private DefaultMutableTreeNode[] nodesToRemove;

	    public TreeTransferHandler() {
	        try {
	            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + DefaultMutableTreeNode[].class.getName() + "\"";
	            nodesFlavor = new DataFlavor(mimeType);
	            flavors[0] = nodesFlavor;
	        } catch(ClassNotFoundException e) {
	            System.out.println("ClassNotFound: " + e.getMessage());
	        }
	    }
		
		public boolean canImport(TransferHandler.TransferSupport support) {
	        if(!support.isDataFlavorSupported(nodesFlavor) || !support.isDrop()) {
	            return false;
	        }
	        support.setShowDropLocation(true);
	        
	        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
	        TreePath dropPath = dl.getPath();
	        DefaultMutableTreeNode target;
	        if(dropPath==null)
	        	target = root;
        	else
        		target = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent();
	        
	        if(target!=root && target.getParent()!=root) // only allow top level transfers
	        	return false;
	        
	        DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
	        
	        // Do not allow a drop on the drag source selections.
	        for(DefaultMutableTreeNode node : selectedNodes)
	        	if(node == target)
	        		return false;
	        // Selections containing strings are unsupported TODO: select children if encountered
	        for(DefaultMutableTreeNode node : selectedNodes)
	        	if(node.getUserObject().getClass() == String.class) 
	        		return false;
	        
	        // Selections containing empty Papa Files are not allowed.
	        for(DefaultMutableTreeNode node : selectedNodes)
	        	if(node.getUserObject().getClass() == PapaFile.class && ((PapaFile)node.getUserObject()).getNumTextures()==0) 
	        		return false;
	        
	        DefaultMutableTreeNode testNode = (DefaultMutableTreeNode) target.getParent();
	        while(testNode!=null) {
	        	for(DefaultMutableTreeNode node : selectedNodes)
	        		if(testNode == node)
	        			return false;
	        	testNode = (DefaultMutableTreeNode) testNode.getParent();
	        }
	        
	        //if(target.getUserObject().getClass() == String.class && target != root)
	        	//return false;
	        
	        if(target.getParent() == null) {
	        	support.setDropAction(MOVE);
	        } else {
	        	support.setDropAction(LINK);
	        }
	        return true;
        }
		
		public int getSourceActions(JComponent c) {
			return MOVE | LINK;
		}

        public boolean importData(TransferHandler.TransferSupport support) {
        	if(!canImport(support)) {
                return false;
            }
            // Extract transfer data.
            DefaultMutableTreeNode[] nodes = null;
            try {
                Transferable t = support.getTransferable();
                nodes = pruneSelectedNodes((DefaultMutableTreeNode[])t.getTransferData(nodesFlavor));
            } catch(UnsupportedFlavorException ufe) {
                System.err.println("UnsupportedFlavor: " + ufe.getMessage());
                return false;
            } catch(IOException ioe) {
                System.err.println("I/O error: " + ioe.getMessage());
                return false;
            }
            
            // Get drop location info.
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
	        TreePath dropPath = dl.getPath();
	        DefaultMutableTreeNode target;
	        if(dropPath==null)
	        	target = root;
        	else
        		target = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent();

            PapaFile targetFile = getAssociatedPapaFile(target);
        	LinkedHashSet<Pair<DefaultMutableTreeNode,PapaFile>> toReload;
        
        
            if(target!=root)
            	toReload = dropTargetPapa(target,targetFile,nodes);
            else
            	toReload = dropTargetGlobal(nodes);
	        
            // I'm not sure why, but not invoking later will cause the JTree to incorrectly refresh leading to a crash. (still true as of v0.2)
            SwingUtilities.invokeLater(() -> toReload.forEach((n) -> refreshEntry(n.getKey(),n.getValue())));
            return true;
        }
        
        private DefaultMutableTreeNode[] pruneSelectedNodes(DefaultMutableTreeNode[] transferData) {
        	ArrayList<DefaultMutableTreeNode> 	nodes 				= new ArrayList<DefaultMutableTreeNode>();
        	ArrayList<DefaultMutableTreeNode> 	nodesToTest 		= new ArrayList<DefaultMutableTreeNode>();
        	ArrayList<DefaultMutableTreeNode> 	nodesToTestUnlinked = new ArrayList<DefaultMutableTreeNode>();
        	ArrayList<PapaTexture> 				linkedTextureList 	= new ArrayList<PapaTexture>();
        	ArrayList<DefaultMutableTreeNode> 	topLevelNodes 		= new ArrayList<DefaultMutableTreeNode>();
        	
        	for(DefaultMutableTreeNode node : transferData) { //filter the files from the textures
        		if(node.getUserObject().getClass() == PapaFile.class) {
        			nodes.add(node);
        			topLevelNodes.add(node);
        		} else {
        			nodesToTest.add(node);
        		}
        	}
        	
        	for(DefaultMutableTreeNode node : nodesToTest) { // get all the linked textures
        		if( ! isChildAny(topLevelNodes, node)) {
	        		PapaTexture tex = (PapaTexture) node.getUserObject();
        			if(tex.isLinked()) {
        				linkedTextureList.add(tex.getLinkedTexture());
        				nodes.add(node);
        			} else {
        				nodesToTestUnlinked.add(node);
        			}
        		}
        	}
        	
        	for(DefaultMutableTreeNode node : nodesToTestUnlinked) {
    			PapaTexture tex = (PapaTexture) node.getUserObject();
    			if( ! tex.isLinked() &&  ! isAnyEqualTo(linkedTextureList, tex))
        			nodes.add(node);
        	}
			return nodes.toArray(new DefaultMutableTreeNode[nodes.size()]);
		}
        
        private boolean isAnyEqualTo(ArrayList<PapaTexture> linkedTextureList, PapaTexture tex) { // skips over calling .equals
			for(PapaTexture t : linkedTextureList)
				if(t == tex)
					return true;
			return false;
		}

		private boolean isChildAny(ArrayList<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode toTest) {
        	for(DefaultMutableTreeNode n : nodes)
				if(n.isNodeDescendant(toTest))
					return true;
        	return false;
        }

		private LinkedHashSet<Pair<DefaultMutableTreeNode,PapaFile>> dropTargetGlobal(DefaultMutableTreeNode[] nodes) {
        	int mode = editor.optionBox("Move selected textures into their own files?", "Rip Settings", new Object[] {"Rip","Copy","Cancel"}, "Rip");
        	LinkedHashSet<Pair<DefaultMutableTreeNode,PapaFile>> toReload = new LinkedHashSet<Pair<DefaultMutableTreeNode,PapaFile>>();
            if(mode == -1 || mode == 2)
            	return toReload;
            boolean rip = mode == 0;
            
            for(DefaultMutableTreeNode node : nodes) {
            	DefaultMutableTreeNode refreshNode = getLowestUnlinkedNode(node);
            	PapaFile associated = getAssociatedPapaFile(refreshNode);
            	PapaFile file = null;
            	Object o = node.getUserObject();
            	DefaultMutableTreeNode[] results = null;
            	if(o instanceof PapaTexture) {
            		PapaTexture t = (PapaTexture)o;
            		file = t.getParent();
            		PapaTexture t2 = extract(t,rip);
            		results = placeInOwnFiles(file, t2);
            		
            	} else {
            		file = (PapaFile)o;
            		PapaTexture[] textures = extract(file,rip);
            		results = placeInOwnFiles(file, textures);
            	}
            	if(rip) {
	            	if(associated==null || (! editor.ALLOW_EMPTY_FILES && associated.getNumTextures()==0) || !associated.containsComponents(~PapaFile.STRING))
            			removeFromTree(refreshNode);
            		toReload.add(new Pair<DefaultMutableTreeNode,PapaFile>(refreshNode, associated));
            	}
            	for(DefaultMutableTreeNode n : results)
        			toReload.add(new Pair<DefaultMutableTreeNode, PapaFile>(n, getAssociatedPapaFile(n)));
            }
            return toReload;
		}

		private LinkedHashSet<Pair<DefaultMutableTreeNode,PapaFile>> 
				dropTargetPapa(DefaultMutableTreeNode target, PapaFile targetFile, DefaultMutableTreeNode[] nodes) {
        	int mode = editor.optionBox("Link selected textures into "+getAssociatedPapaFile(target)+"?", "Link Settings", new Object[] {"Link","Embed","Cancel"}, "Link");
            
        	LinkedHashSet<Pair<DefaultMutableTreeNode,PapaFile>> toReload = new LinkedHashSet<Pair<DefaultMutableTreeNode,PapaFile>>();
        	
            if(mode == -1 || mode == 2)
            	return toReload;
            
            boolean link = mode == 0;
            if(link && PapaFile.getPlanetaryAnnihilationDirectory()==null) {
            	if(editor.showError("Link is unavailable until the media directory is set.", "Cannot link", new Object[] {"Ok","Set Media Directory"}, "Ok") == 1)
            		editor.menu.changeMediaDirectory();
            	if(PapaFile.getPlanetaryAnnihilationDirectory()==null)
            		return toReload;
            }
            for(DefaultMutableTreeNode node : nodes) {
            	DefaultMutableTreeNode refreshNode = getLowestUnlinkedNode(node);
            	PapaFile associated = getAssociatedPapaFile(refreshNode);
            	PapaFile file = null;
            	Object o = node.getUserObject();
            	
            	if(o instanceof PapaTexture) {
            		PapaTexture t = (PapaTexture)o;
            		file = t.getParent();
            		t = extract(t, true);
            		AttachToNewFile(targetFile, link, t);
            		
            	} else {
            		file = (PapaFile)o;
            		PapaTexture[] textures = extract(file,true);
            		AttachToNewFile(targetFile, link, textures);
            	}
            	if(associated== null || (! editor.ALLOW_EMPTY_FILES && associated.getNumTextures()==0) || !associated.containsComponents(~PapaFile.STRING))
        			removeFromTree(refreshNode);
        		toReload.add(new Pair<DefaultMutableTreeNode,PapaFile>(refreshNode, associated));
            }
            toReload.add(new Pair<DefaultMutableTreeNode,PapaFile>(target, targetFile));
            return toReload;
            
		}
		
		private DefaultMutableTreeNode[] placeInOwnFiles(PapaFile source, PapaTexture... textures) {
			ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
        	for(PapaTexture t : textures) {
        		if(t==null)
        			continue;
    			PapaFile p = source.getEmptyCopy();
    			t.attach(p);
    			nodes.add(addToTree(p, false));
        	}
        	return nodes.toArray(new DefaultMutableTreeNode[nodes.size()]);
        }
        
        private PapaTexture[] extract(PapaFile p, boolean rip) {
        	PapaTexture[] textures = getValidTextures(p);
        	HashSet<PapaTexture> uniqueTextures = new HashSet<PapaTexture>();
        	for(int i =0;i<textures.length;i++) {
    			uniqueTextures.add(extract(textures[i],rip));
        	}
        	return uniqueTextures.toArray(new PapaTexture[uniqueTextures.size()]);
        }
        
        private PapaTexture[] getValidTextures(PapaFile p) {
        	ArrayList<PapaTexture> textures = new ArrayList<PapaTexture>();
        	for(int i = 0; i <p.getNumTextures(); i++) { //TODO: the order of this actually matters. Fundamental problems with detach...
        		PapaTexture t = p.getTexture(i);
        		if(t.isLinked() && ! t.linkValid())
        			continue;
        		textures.add(t);
        	}
        	return textures.toArray(new PapaTexture[textures.size()]);
        }
        
        private PapaTexture extract(PapaTexture t,boolean rip) {
        	PapaTexture target = t;
        	if(target.isLinked()) {
        		if(target.getParent() == null || ! target.linkValid())
        			return null;
        		target = target.getLinkedTexture();
        	}
        	if(rip) {
        		if(t!=target) { // overwrite the linked texture's name if it was not the initial target of the operation
        			t.detach();
	        		target.setName(t.getName());
        		}
        		PapaFile parent = target.getParent();
        		if(parent!=null && parent.isLinkedFile())
        			parent.detach();
        		target.detach();
        		return target;
        	}
        	return target.duplicate();
        }

		private void AttachToNewFile(PapaFile file, boolean link, PapaTexture... textures) {
			for(PapaTexture t : textures) {
				if(t==null)
					continue;
    			if(link) {
    				if(!t.getName().startsWith("/"))
    					t.setName("/"+t.getName()); // add implicit /
    				file.generateLinkedTexture(t);
    			}
    			else
    				t.attach(file);
			}
		}

		@Override
        protected Transferable createTransferable(JComponent c) {
        	DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
            if(selectedNodes != null) {
                return new NodesTransferable(selectedNodes);
            }
            return null;
        }
        
        private class NodesTransferable implements Transferable {
	        DefaultMutableTreeNode[] nodes;

	        public NodesTransferable(DefaultMutableTreeNode[] nodes) {
	            this.nodes = nodes;
	         }

	        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
	            if(!isDataFlavorSupported(flavor))
	                throw new UnsupportedFlavorException(flavor);
	            return nodes;
	        }

	        public DataFlavor[] getTransferDataFlavors() {
	            return flavors;
	        }

	        public boolean isDataFlavorSupported(DataFlavor flavor) {
	            return nodesFlavor.equals(flavor);
	        }
	    }
    }

}