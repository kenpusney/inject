package net.kimleo.inject.context;

public class QualifiedComponent {
    private final String qualifier;
    private final Object object;

    public QualifiedComponent(String qualifier, Object object) {
        this.qualifier = qualifier;
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QualifiedComponent that = (QualifiedComponent) o;

        if (qualifier != null ? !qualifier.equals(that.qualifier) : that.qualifier != null) return false;
        return object != null ? object.getClass().equals(that.object.getClass()) : that.object == null;

    }

    @Override
    public int hashCode() {
        int result = qualifier != null ? qualifier.hashCode() : 0;
        result = 31 * result + (object != null ? object.getClass().hashCode() : 0);
        return result;
    }

    public String getQualifier() {
        return qualifier;
    }

    public Object getObject() {
        return object;
    }
}
