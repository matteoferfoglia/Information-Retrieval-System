open module it.units.informationretrieval.ir_boolean_model {

    requires com.fasterxml.jackson.core;
    requires org.jetbrains.annotations;
    requires java.logging;
    requires org.apache.commons.collections4;
    requires com.fasterxml.jackson.databind;
    requires skiplist;
    requires edit_distance;
    requires jbool.expressions;
    requires benchmark;
    requires java.desktop;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.swing;

    exports it.units.informationretrieval.ir_boolean_model.entities;
    exports it.units.informationretrieval.ir_boolean_model.queries;
    exports it.units.informationretrieval.ir_boolean_model.utils;
    exports it.units.informationretrieval.ir_boolean_model.exceptions;
    exports it.units.informationretrieval.ir_boolean_model;
    exports it.units.informationretrieval.ir_boolean_model.utils.stemmers;
    exports it.units.informationretrieval.ir_boolean_model.plots;
    exports it.units.informationretrieval.ir_boolean_model.factories;
    exports it.units.informationretrieval.ir_boolean_model.utils.functional;
    exports it.units.informationretrieval.ir_boolean_model.utils.custom_types;

}