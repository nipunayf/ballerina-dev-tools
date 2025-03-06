package io.ballerina.servicemodelgenerator.extension.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DatabaseManager;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.modelgenerator.commons.ServiceDeclaration;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.DisplayAnnotation;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServiceModelUtils {

    public static Optional<Service> getEmptyServiceModel(String moduleName) {
        if (moduleName.equals("http")) {
            return getHttpService();
        }
        Optional<ServiceDeclaration> serviceDeclaration = ServiceDatabaseManager.getInstance()
                .getServiceDeclaration(moduleName);
        if (serviceDeclaration.isEmpty()) {
            return Optional.empty();
        }
        ServiceDeclaration serviceTemplate = serviceDeclaration.get();
        ServiceDeclaration.Package pkg = serviceTemplate.packageInfo();

        String protocol = getProtocol(moduleName);

        // TODO: introduce a new label field
        String label = protocol + " Service";
        String documentation = "Add the service documentation";
        String icon = CommonUtils.generateIcon(pkg.org(), pkg.name(), pkg.version());

        Map<String, Value> properties = new LinkedHashMap<>();

        Service.ServiceModelBuilder serviceBuilder = new Service.ServiceModelBuilder();
        serviceBuilder
                .setId(String.valueOf(pkg.packageId()))
                .setName(label)
                .setType(moduleName)
                .setDisplayName(label)
                .setDescription(documentation)
                .setDisplayAnnotation(new DisplayAnnotation(label, icon))
                .setModuleName(moduleName)
                .setOrgName(pkg.org())
                .setVersion(pkg.version())
                .setPackageName(pkg.name())
                .setListenerProtocol(protocol)
                .setIcon(icon)
                .setProperties(properties)
                .setFunctions(List.of(getInitFunction()));

        Service service = serviceBuilder.build();
        properties.put("listener", getListenersProperty(protocol, serviceTemplate.listenerKind()));

        // type descriptor
        properties.put("serviceType", getTypeDescriptorProperty(serviceTemplate, pkg.packageId()));

        // base path
        if (serviceTemplate.optionalAbsoluteResourcePath() == 0) {
            properties.put("basePath", getBasePathProperty(serviceTemplate));
        }

        // string literal
        if (serviceTemplate.optionalStringLiteral() == 0) {
            properties.put("stringLiteral", getStringLiteral(serviceTemplate));
        }


        return Optional.of(service);
    }

    public static Function getInitFunction() {
        Value.ValueBuilder functionName = new Value.ValueBuilder();
        functionName
                .setMetadata(new MetaData("Init Function", "The init function"))
                .setCodedata(new Codedata("FUNCTION_NAME"))
                .setValue("init")
                .setValueType("IDENTIFIER")
                .setValueTypeConstraint("string")
                .setPlaceholder("init")
                .setEnabled(true)
                .setEditable(false)
                .setType(false)
                .setOptional(false)
                .setAdvanced(false);

        Value.ValueBuilder functionReturnType = new Value.ValueBuilder();
        functionReturnType
                .setMetadata(new MetaData("Return Type", "The return type of the function"))
                .setValue("error?")
                .setValueType("TYPE")
                .setValueTypeConstraint("string")
                .setPlaceholder("error?")
                .setEnabled(true)
                .setEditable(true)
                .setType(true)
                .setOptional(true)
                .setAdvanced(false);

        Function.FunctionBuilder functionBuilder = new Function.FunctionBuilder();
        functionBuilder
                .setMetadata(new MetaData("Init", "The Init function of the service"))
                .setKind("DEFAULT")
                .setEnabled(true)
                .setOptional(false)
                .setEditable(true)
                .setName(functionName.build())
                .setReturnType(new FunctionReturnType(functionReturnType.build()));

        return functionBuilder.build();
    }

    private static Value getTypeDescriptorProperty(ServiceDeclaration template, int packageId) {
        List<String> serviceTypes = ServiceDatabaseManager.getInstance().getServiceTypes(packageId);
        String value = "";
        if (serviceTypes.size() == 1) {
            value = serviceTypes.getFirst();
        }
        List<String> items = new ArrayList<>();
        items.add("");
        items.addAll(serviceTypes);

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.typeDescriptorLabel(), template.typeDescriptorDescription()))
                .setCodedata(new Codedata("SERVICE_TYPE"))
                .setValue(value)
                .setItems(items)
                .setValueType("SINGLE_SELECT")
                .setValueTypeConstraint("string")
                .setPlaceholder(template.typeDescriptorDefaultValue())
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(template.optionalTypeDescriptor() == 0)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    private static Value getStringLiteral(ServiceDeclaration template) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.stringLiteralLabel(), template.stringLiteralDescription()))
                .setCodedata(new Codedata("STRING_LITERAL"))
                .setValue("")
                .setValues(new ArrayList<>())
                .setValueType("EXPRESSION")
                .setValueTypeConstraint("string")
                .setPlaceholder(template.stringLiteralDefaultValue())
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    private static Value getBasePathProperty(ServiceDeclaration template) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.absoluteResourcePathLabel(),
                        template.absoluteResourcePathDescription()))
                .setCodedata(new Codedata("SERVICE_BASE_PATH"))
                .setValue("")
                .setValues(new ArrayList<>())
                .setValueType("EXPRESSION") // introduce a new type for validation purposes
                .setValueTypeConstraint("string")
                .setPlaceholder(template.absoluteResourcePathDefaultValue())
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    private static Value getListenersProperty(String protocol, String valueType) {
        boolean isMultiple = valueType.equals("MULTIPLE_SELECT");
        MetaData metaData = isMultiple ?
                new MetaData("Listeners", "The Listeners to be bound with the service")
                : new MetaData("Listener", "The Listener to be bound with the service");

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(metaData)
                .setCodedata(new Codedata("LISTENER"))
                .setValue("")
                .setValues(new ArrayList<>())
                .setValueType(valueType)
                .setValueTypeConstraint(protocol+ ":" + "Listener")
                .setPlaceholder("")
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(isMultiple);

        return valueBuilder.build();
    }

    public static String getProtocol(String moduleName) {
        String[] split = moduleName.split("\\.");
        return split[split.length - 1];
    }


    public static Optional<Service> getHttpService() {
        InputStream resourceStream = ServiceModelUtils.class.getClassLoader()
                .getResourceAsStream("services/http.json");
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Service.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
