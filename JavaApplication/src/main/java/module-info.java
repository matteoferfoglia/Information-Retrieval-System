module it.units.informationretrieval.ir_boolean_model {
    requires com.fasterxml.jackson.core;
    requires org.jetbrains.annotations;
    requires java.logging;
    requires org.apache.commons.collections4;
    requires com.fasterxml.jackson.databind;

    opens it.units.informationretrieval.ir_boolean_model.entities to java.logging;

    exports it.units.informationretrieval.ir_boolean_model.document_descriptors;
    exports it.units.informationretrieval.ir_boolean_model.entities;
    exports it.units.informationretrieval.ir_boolean_model.queries;
    exports it.units.informationretrieval.ir_boolean_model.utils;
}