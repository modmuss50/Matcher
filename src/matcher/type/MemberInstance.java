package matcher.type;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import org.objectweb.asm.Opcodes;

import matcher.NameType;
import matcher.Util;

public abstract class MemberInstance<T extends MemberInstance<T>> implements Matchable<T> {
	@SuppressWarnings("unchecked")
	protected MemberInstance(ClassInstance cls, String id, String origName, boolean nameObfuscated, int position, boolean isStatic) {
		this.cls = cls;
		this.id = id;
		this.origName = origName;
		this.nameObfuscated = nameObfuscated;
		this.position = position;
		this.isStatic = isStatic;

		if (cls.isShared()) {
			matchedInstance = (T) this;
		}
	}

	public ClassInstance getCls() {
		return cls;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public final String getName() {
		return origName;
	}

	@Override
	public String getName(NameType type) {
		if (type == NameType.PLAIN) {
			return origName;
		} else if (type == NameType.UID_PLAIN) {
			int uid = getUid();
			if (uid >= 0) return getUidString();
		}

		boolean mapped = type == NameType.MAPPED_PLAIN || type == NameType.MAPPED_TMP_PLAIN || type == NameType.MAPPED_LOCTMP_PLAIN;
		boolean tmp = type == NameType.MAPPED_TMP_PLAIN || type == NameType.TMP_PLAIN;
		boolean locTmp = type == NameType.MAPPED_LOCTMP_PLAIN || type == NameType.LOCTMP_PLAIN;
		String ret;

		if (mapped && mappedName != null) {
			// MAPPED_*, local name available
			ret = mappedName;
		} else if (mapped && matchedInstance != null && matchedInstance.mappedName != null) {
			// MAPPED_*, remote name available
			ret = matchedInstance.mappedName;
		} else if (tmp && (nameObfuscated || !mapped) && matchedInstance != null && matchedInstance.tmpName != null) {
			// MAPPED_TMP_* with obf name or TMP_*, remote name available
			ret = matchedInstance.tmpName;
		} else if ((tmp || locTmp) && (nameObfuscated || !mapped) && tmpName != null) {
			// MAPPED_TMP_* or MAPPED_LOCTMP_* with obf name or TMP_* or LOCTMP_*, local name available
			ret = tmpName;
		} else {
			ret = origName;
		}

		return ret;
	}

	@Override
	public String getDisplayName(NameType type, boolean full) {
		String name = getName(type);

		if (full) {
			return cls.getDisplayName(type, full) + "." + name;
		} else {
			return name;
		}
	}

	public abstract String getDesc();
	public abstract boolean isReal();

	@Override
	public ClassEnv getEnv() {
		return cls.getEnv();
	}

	@Override
	public boolean isNameObfuscated() {
		return nameObfuscated;
	}

	public int getPosition() {
		return position;
	}

	public abstract int getAccess();

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isFinal() {
		return (getAccess() & Opcodes.ACC_FINAL) != 0;
	}

	void addParent(T parent) {
		assert parent.getCls() != getCls();
		assert parent != this;
		assert !children.contains(parent);

		if (parents.isEmpty()) parents = Util.newIdentityHashSet();

		parents.add(parent);
	}

	public Set<T> getParents() {
		return parents;
	}

	void addChild(T child) {
		assert child.getCls() != getCls();
		assert child != this;
		assert !parents.contains(child);

		if (children.isEmpty()) children = Util.newIdentityHashSet();

		children.add(child);
	}

	public Set<T> getChildren() {
		return children;
	}

	@SuppressWarnings("unchecked")
	public T getMatchedHierarchyMember() {
		if (getMatch() != null) return (T) this;

		ClassEnv reqEnv = cls.getEnv();

		for (T m : hierarchyMembers) {
			if (m.getMatch() != null) {
				ClassEnv env = m.cls.getEnv();

				if (env.isShared() || env == reqEnv) return m;
			}
		}

		return null;
	}

	public Set<T> getAllHierarchyMembers() {
		assert hierarchyMembers != null;

		return hierarchyMembers;
	}

	@Override
	public boolean hasLocalTmpName() {
		return tmpName != null;
	}

	public void setTmpName(String tmpName) {
		this.tmpName = tmpName;
	}

	@Override
	public int getUid() {
		if (uid >= 0) {
			if (matchedInstance != null && matchedInstance.uid >= 0) {
				return Math.min(uid, matchedInstance.uid);
			} else {
				return uid;
			}
		} else if (matchedInstance != null) {
			return matchedInstance.uid;
		} else {
			return -1;
		}
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	protected abstract String getUidString();

	@Override
	public boolean hasMappedName() {
		return mappedName != null || matchedInstance != null && matchedInstance.mappedName != null;
	}

	public void setMappedName(String mappedName) {
		this.mappedName = mappedName;
	}

	public String getMappedComment() {
		if (mappedComment != null) {
			return mappedComment;
		} else if (matchedInstance != null) {
			return matchedInstance.mappedComment;
		} else {
			return null;
		}
	}

	public void setMappedComment(String comment) {
		if (comment != null && comment.isEmpty()) comment = null;

		this.mappedComment = comment;
	}

	@Override
	public T getMatch() {
		return matchedInstance;
	}

	public void setMatch(T match) {
		assert match == null || cls == match.cls.getMatch();

		this.matchedInstance = match;
	}

	@Override
	public String toString() {
		return getDisplayName(NameType.PLAIN, true);
	}

	public static final Comparator<MemberInstance<?>> nameComparator = Comparator.<MemberInstance<?>, String>comparing(MemberInstance::getName).thenComparing(MemberInstance::getDesc);

	final ClassInstance cls;
	final String id;
	final String origName;
	boolean nameObfuscated;
	final int position;
	final boolean isStatic;

	private Set<T> parents = Collections.emptySet();
	private Set<T> children = Collections.emptySet();
	Set<T> hierarchyMembers;

	String tmpName;
	int uid = -1;

	String mappedName;
	String mappedComment;
	T matchedInstance;
}
