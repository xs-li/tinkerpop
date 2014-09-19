package com.tinkerpop.gremlin.structure.util.detached;

import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.MetaProperty;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Optional;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class DetachedProperty<V> implements Property, Serializable, Attachable<Property<V>> {

    String key;
    V value;
    transient DetachedElement element;

    private DetachedProperty() {

    }

    public DetachedProperty(final String key, final V value, final DetachedElement element) {
        if (null == key) throw Graph.Exceptions.argumentCanNotBeNull("key");
        if (null == value) throw Graph.Exceptions.argumentCanNotBeNull("value");
        if (null == element) throw Graph.Exceptions.argumentCanNotBeNull("element");

        this.key = key;
        this.value = value;
        this.element = element;
    }

    // todo: straighten out all these constructors and their scopes - what do we really need here?

    private DetachedProperty(final Property property) {
        if (null == property) throw Graph.Exceptions.argumentCanNotBeNull("property");

        this.key = property.isHidden() ? Graph.Key.hide(property.key()) : property.key();
        this.value = (V) property.value();
        final Element element = property.getElement();

        if (element instanceof Vertex)
            this.element = element instanceof DetachedVertex ? (DetachedElement) element : DetachedVertex.detach((Vertex) element);
        else if (element instanceof MetaProperty)
            this.element = element instanceof DetachedMetaProperty ? (DetachedElement) element : DetachedMetaProperty.detach((MetaProperty) element);
        else
            this.element = element instanceof DetachedEdge ? (DetachedElement) element : DetachedEdge.detach((Edge) element);
    }

    DetachedProperty(final Property property, final DetachedEdge element) {
        if (null == property) throw Graph.Exceptions.argumentCanNotBeNull("property");

        this.key = property.isHidden() ? Graph.Key.hide(property.key()) : property.key();
        this.value = (V) property.value();
        this.element = element;
    }

    DetachedProperty(final Property property, final DetachedMetaProperty element) {
        if (null == property) throw Graph.Exceptions.argumentCanNotBeNull("property");

        this.key = property.isHidden() ? Graph.Key.hide(property.key()) : property.key();
        this.value = (V) property.value();
        this.element = element;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return Graph.Key.isHidden(this.key);
    }

    @Override
    public String key() {
        return Graph.Key.unHide(this.key);
    }

    @Override
    public V value() {
        return this.value;
    }

    @Override
    public Element getElement() {
        return this.element;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Detached properties are readonly: " + this.toString());
    }

    @Override
    public String toString() {
        return StringFactory.propertyString(this);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(final Object object) {
        return ElementHelper.areEqual(this, object);
    }

    @Override
    public int hashCode() {
        return this.element.id.hashCode() + this.key.hashCode() + this.value.hashCode();
    }

    @Override
    public Property<V> attach(final Vertex hostVertex) {
        if (this.getElement() instanceof Vertex) {
            return Optional.<Property<V>>of(hostVertex.property(this.key)).orElseThrow(() -> new IllegalStateException("The detached property could not be be found at the provided vertex: " + this));
        } else {
            final String label = this.getElement().label();
            final Object id = this.getElement().id();
            return StreamFactory.stream((Iterator<Edge>) hostVertex.outE(label))
                    .filter(e -> e.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("The detached property could not be be found at the provided vertex's edges: " + this))
                    .property(this.key());

        }
    }

    @Override
    public Property<V> attach(final Graph hostGraph) {
        final Element element = (this.getElement() instanceof Vertex) ?
                hostGraph.v(this.getElement().id()) :
                hostGraph.e(this.getElement().id());
        return Optional.<Property<V>>of(element.property(this.key)).orElseThrow(() -> new IllegalStateException("The detached property could not be found in the provided graph: " + this));
    }

    public static DetachedProperty detach(final Property property) {
        if (null == property) throw Graph.Exceptions.argumentCanNotBeNull("property");
        if (property instanceof DetachedProperty) throw new IllegalArgumentException("Property is already detached");
        return new DetachedProperty(property);
    }
}
