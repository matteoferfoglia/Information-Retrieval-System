open module it.units.informationretrieval.ir_boolean_model {

    requires com.fasterxml.jackson.core;
    requires org.jetbrains.annotations;
    requires java.logging;
    requires org.apache.commons.collections4;
    requires com.fasterxml.jackson.databind;
    requires skiplist;
    requires edit_distance;
    requires jbool.expressions;

    exports it.units.informationretrieval.ir_boolean_model.document_descriptors;
    exports it.units.informationretrieval.ir_boolean_model.entities;
    exports it.units.informationretrieval.ir_boolean_model.queries;
    exports it.units.informationretrieval.ir_boolean_model.utils;
    exports it.units.informationretrieval.ir_boolean_model.exceptions;
    exports it.units.informationretrieval.ir_boolean_model;
    exports it.units.informationretrieval.ir_boolean_model.utils.stemmers;

}