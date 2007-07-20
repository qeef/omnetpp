package org.omnetpp.scave.editors;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.ScavePlugin;


/**
 * State of the scave editor that is saved when the editor is closed
 * and restored when opened.
 * <p>
 * It is saved in persistent properties of the input file of the editor.
 * 
 * @author tomi
 */
public class ScaveEditorMemento implements IMemento
{
	/* Name of the root element of the XML memento */
	private static final String ROOT_ELEMENT = "ScaveState";
	
	/* Names and keys used for persistent properties*/
	private static final String SCAVE_STATE = "ScaveState";
	private static final String CHUNK_COUNT = "ScaveStateChunkCount";
	private static final String CHUNK = "ScaveStateChunk";
	private static final String QUALIFIER = ScavePlugin.PLUGIN_ID;
	private static final QualifiedName KEY_CHUNK_COUNT = new QualifiedName(QUALIFIER, CHUNK_COUNT);
	private static final QualifiedName KEY_SCAVE_STATE = new QualifiedName(QUALIFIER, SCAVE_STATE);
	
	/* The IMemento methods are delegated to this XMLMemento */
	private XMLMemento memento;
	
	public ScaveEditorMemento() {
		memento = XMLMemento.createWriteRoot(ROOT_ELEMENT);
	}
	
	public ScaveEditorMemento(IFile file) throws CoreException {
		load(file);
	}

	public IMemento createChild(String type, String id) {
		return memento.createChild(type, id);
	}

	public IMemento createChild(String type) {
		return memento.createChild(type);
	}

	public boolean equals(Object obj) {
		return memento.equals(obj);
	}

	public IMemento getChild(String type) {
		return memento.getChild(type);
	}

	public IMemento[] getChildren(String type) {
		return memento.getChildren(type);
	}

	public Float getFloat(String key) {
		return memento.getFloat(key);
	}

	public String getID() {
		return memento.getID();
	}

	public Integer getInteger(String key) {
		return memento.getInteger(key);
	}

	public String getString(String key) {
		return memento.getString(key);
	}

	public String getTextData() {
		return memento.getTextData();
	}

	public int hashCode() {
		return memento.hashCode();
	}

	public void putFloat(String key, float f) {
		memento.putFloat(key, f);
	}

	public void putInteger(String key, int n) {
		memento.putInteger(key, n);
	}

	public void putMemento(IMemento memento) {
		memento.putMemento(memento);
	}

	public void putString(String key, String value) {
		memento.putString(key, value);
	}

	public void putTextData(String data) {
		memento.putTextData(data);
	}

	public void save(IFile file) throws CoreException {
		try {
			StringWriter writer = new StringWriter();
			memento.save(writer);
			String[] chunks = StringUtils.split(writer.toString(), 2000);
			if (chunks.length == 1) {
				file.setPersistentProperty(KEY_SCAVE_STATE, chunks[0]);
			}
			else {
				file.setPersistentProperty(KEY_CHUNK_COUNT, String.valueOf(chunks.length));
				for (int i = 0; i < chunks.length; ++i) {
					QualifiedName key = new QualifiedName(QUALIFIER, CHUNK+"."+i);
					file.setPersistentProperty(key, chunks[i]);
				}
			}
		} catch (Exception e) {
			IStatus error = new Status(IStatus.ERROR, ScavePlugin.PLUGIN_ID, 0,
									"Cannot save Scave editor state.", e);
			throw new CoreException(error);
		}
	}
	
	protected void load(IFile file) throws CoreException {
		try {
			String state = file.getPersistentProperty(KEY_SCAVE_STATE);
			
			if (state == null) {
				int count = getPersistentIntProperty(file, KEY_CHUNK_COUNT);
				if (count >= 0 && count < 100) { // limit state to ~200k
					String[] chunks = new String[count];
					for (int i = 0; i < count; ++i) {
						QualifiedName key = new QualifiedName(QUALIFIER, CHUNK+"."+i);
						String chunk = file.getPersistentProperty(key);
						if (chunk == null)
							throw new Exception("Scave editor state is corrupted.");
						chunks[i] = chunk;
					}
					state = StringUtils.join(chunks);
				}
				else
					throw new Exception("Scave editor state is corrupted.");
			}
			if (state != null) {
				Reader reader = new StringReader(state);
				memento = XMLMemento.createReadRoot(reader);
			}
		} catch (Exception e) {
			IStatus error = new Status(IStatus.ERROR, ScavePlugin.PLUGIN_ID, 0,
					"Cannot load Scave editor state.", e);
			throw new CoreException(error);
		}
		
		if (memento == null) {
			IStatus error = new Status(IStatus.WARNING, ScavePlugin.PLUGIN_ID, 0,
					String.format("No Scave editor state was saved for file: %s.", file.toString()), null);
			throw new CoreException(error);
		}
	}

	public String toString() {
		return memento.toString();
	}
	
	private int getPersistentIntProperty(IFile file, QualifiedName key) throws CoreException {
		try {
			return Integer.parseInt(file.getPersistentProperty(key));
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
