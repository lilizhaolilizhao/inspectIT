package rocks.inspectit.shared.cs.storage.serializer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.EnumSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import rocks.inspectit.shared.all.serializer.impl.CustomCompatibleFieldSerializer;
import rocks.inspectit.shared.all.serializer.impl.SerializationManager;
import rocks.inspectit.shared.all.serializer.schema.ClassSchemaManager;
import rocks.inspectit.shared.cs.ci.AgentMapping;
import rocks.inspectit.shared.cs.ci.AgentMappings;
import rocks.inspectit.shared.cs.ci.AlertingDefinition;
import rocks.inspectit.shared.cs.ci.BusinessContextDefinition;
import rocks.inspectit.shared.cs.ci.Environment;
import rocks.inspectit.shared.cs.ci.Profile;
import rocks.inspectit.shared.cs.ci.assignment.impl.ChartingMethodSensorAssignment;
import rocks.inspectit.shared.cs.ci.assignment.impl.ExceptionSensorAssignment;
import rocks.inspectit.shared.cs.ci.assignment.impl.InvocationStartMethodSensorAssignment;
import rocks.inspectit.shared.cs.ci.assignment.impl.JmxBeanSensorAssignment;
import rocks.inspectit.shared.cs.ci.assignment.impl.MethodSensorAssignment;
import rocks.inspectit.shared.cs.ci.assignment.impl.TimerMethodSensorAssignment;
import rocks.inspectit.shared.cs.ci.business.expression.AbstractExpression;
import rocks.inspectit.shared.cs.ci.business.expression.impl.AndExpression;
import rocks.inspectit.shared.cs.ci.business.expression.impl.BooleanExpression;
import rocks.inspectit.shared.cs.ci.business.expression.impl.NameExtractionExpression;
import rocks.inspectit.shared.cs.ci.business.expression.impl.NotExpression;
import rocks.inspectit.shared.cs.ci.business.expression.impl.OrExpression;
import rocks.inspectit.shared.cs.ci.business.expression.impl.StringMatchingExpression;
import rocks.inspectit.shared.cs.ci.business.impl.ApplicationDefinition;
import rocks.inspectit.shared.cs.ci.business.impl.BusinessTransactionDefinition;
import rocks.inspectit.shared.cs.ci.business.valuesource.PatternMatchingType;
import rocks.inspectit.shared.cs.ci.business.valuesource.StringValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.AgentNameValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HostValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpParameterValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpQueryStringValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpRequestMethodValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpSchemeValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpServerNameValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpServerPortValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpUriValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.HttpUrlValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.MethodParameterValueSource;
import rocks.inspectit.shared.cs.ci.business.valuesource.impl.MethodSignatureValueSource;
import rocks.inspectit.shared.cs.ci.context.impl.FieldContextCapture;
import rocks.inspectit.shared.cs.ci.context.impl.ParameterContextCapture;
import rocks.inspectit.shared.cs.ci.context.impl.ReturnContextCapture;
import rocks.inspectit.shared.cs.ci.eum.EndUserMonitoringConfig;
import rocks.inspectit.shared.cs.ci.exclude.ExcludeRule;
import rocks.inspectit.shared.cs.ci.export.ConfigurationInterfaceImportData;
import rocks.inspectit.shared.cs.ci.profile.data.ExcludeRulesProfileData;
import rocks.inspectit.shared.cs.ci.profile.data.JmxDefinitionProfileData;
import rocks.inspectit.shared.cs.ci.profile.data.SensorAssignmentProfileData;
import rocks.inspectit.shared.cs.ci.sensor.exception.impl.ExceptionSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.jmx.JmxSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.ConnectionSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.ExecutorClientSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.HttpSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.InvocationSequenceSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.Log4jLoggingSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.ApacheClientExchangeHandlerSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.PreparedStatementParameterSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.PreparedStatementSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteApacheHttpClientV40SensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteAsyncApacheHttpClientSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteJavaHttpServerSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteJettyHttpClientV61ClientSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteJmsClientSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteJmsListenerServerSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteManualServerSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteSpringRestTemplateClientSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.RemoteUrlConnectionClientSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.StatementSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.impl.TimerSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.method.special.impl.ExecutorIntercepterSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.platform.impl.ClassLoadingSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.platform.impl.CompilationSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.platform.impl.CpuSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.platform.impl.MemorySensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.platform.impl.RuntimeSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.platform.impl.SystemSensorConfig;
import rocks.inspectit.shared.cs.ci.sensor.platform.impl.ThreadSensorConfig;
import rocks.inspectit.shared.cs.ci.strategy.impl.DisruptorStrategyConfig;
import rocks.inspectit.shared.cs.cmr.property.configuration.Configuration;
import rocks.inspectit.shared.cs.cmr.property.configuration.GroupedProperty;
import rocks.inspectit.shared.cs.cmr.property.configuration.PropertySection;
import rocks.inspectit.shared.cs.cmr.property.configuration.impl.BooleanProperty;
import rocks.inspectit.shared.cs.cmr.property.configuration.impl.ByteProperty;
import rocks.inspectit.shared.cs.cmr.property.configuration.impl.LongProperty;
import rocks.inspectit.shared.cs.cmr.property.configuration.impl.PercentageProperty;
import rocks.inspectit.shared.cs.cmr.property.configuration.impl.StringProperty;
import rocks.inspectit.shared.cs.cmr.property.configuration.validation.PropertyValidation;
import rocks.inspectit.shared.cs.cmr.property.configuration.validation.PropertyValidationException;
import rocks.inspectit.shared.cs.cmr.property.configuration.validation.ValidationError;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.EMailListValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.EMailValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.FullyQualifiedClassNameValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.GreaterOrEqualValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.GreaterValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.LessOrEqualValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.LessValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.NegativeValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.NotEmptyValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.PercentageValidator;
import rocks.inspectit.shared.cs.cmr.property.configuration.validator.impl.PositiveValidator;
import rocks.inspectit.shared.cs.cmr.property.update.configuration.ConfigurationUpdate;
import rocks.inspectit.shared.cs.cmr.property.update.impl.BooleanPropertyUpdate;
import rocks.inspectit.shared.cs.cmr.property.update.impl.BytePropertyUpdate;
import rocks.inspectit.shared.cs.cmr.property.update.impl.LongPropertyUpdate;
import rocks.inspectit.shared.cs.cmr.property.update.impl.PercentagePropertyUpdate;
import rocks.inspectit.shared.cs.cmr.property.update.impl.RestoreDefaultPropertyUpdate;
import rocks.inspectit.shared.cs.cmr.property.update.impl.StringPropertyUpdate;
import rocks.inspectit.shared.cs.cmr.service.IServerStatusService.ServerStatus;
import rocks.inspectit.shared.cs.communication.comparator.AggregatedExceptionSensorDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.DefaultDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.ExceptionSensorDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.HttpTimerDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.InvocationAwareDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.InvocationSequenceDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.MethodSensorDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.ResultComparator;
import rocks.inspectit.shared.cs.communication.comparator.SqlStatementDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.comparator.TimerDataComparatorEnum;
import rocks.inspectit.shared.cs.communication.data.cmr.Alert;
import rocks.inspectit.shared.cs.communication.data.cmr.AlertClosingReason;
import rocks.inspectit.shared.cs.communication.data.cmr.RecordingData;
import rocks.inspectit.shared.cs.communication.data.cmr.WritingStatus;
import rocks.inspectit.shared.cs.indexing.aggregation.impl.ExceptionDataAggregator;
import rocks.inspectit.shared.cs.indexing.aggregation.impl.HttpTimerDataAggregator;
import rocks.inspectit.shared.cs.indexing.aggregation.impl.SqlStatementDataAggregator;
import rocks.inspectit.shared.cs.indexing.aggregation.impl.TimerDataAggregator;
import rocks.inspectit.shared.cs.indexing.indexer.impl.InvocationChildrenIndexer;
import rocks.inspectit.shared.cs.indexing.indexer.impl.MethodIdentIndexer;
import rocks.inspectit.shared.cs.indexing.indexer.impl.ObjectTypeIndexer;
import rocks.inspectit.shared.cs.indexing.indexer.impl.PlatformIdentIndexer;
import rocks.inspectit.shared.cs.indexing.indexer.impl.SensorTypeIdentIndexer;
import rocks.inspectit.shared.cs.indexing.indexer.impl.SqlStringIndexer;
import rocks.inspectit.shared.cs.indexing.indexer.impl.TimestampIndexer;
import rocks.inspectit.shared.cs.indexing.storage.impl.ArrayBasedStorageLeaf;
import rocks.inspectit.shared.cs.indexing.storage.impl.LeafWithNoDescriptors;
import rocks.inspectit.shared.cs.indexing.storage.impl.SimpleStorageDescriptor;
import rocks.inspectit.shared.cs.indexing.storage.impl.StorageBranch;
import rocks.inspectit.shared.cs.indexing.storage.impl.StorageBranchIndexer;
import rocks.inspectit.shared.cs.storage.LocalStorageData;
import rocks.inspectit.shared.cs.storage.StorageData;
import rocks.inspectit.shared.cs.storage.StorageData.StorageState;
import rocks.inspectit.shared.cs.storage.label.BooleanStorageLabel;
import rocks.inspectit.shared.cs.storage.label.DateStorageLabel;
import rocks.inspectit.shared.cs.storage.label.NumberStorageLabel;
import rocks.inspectit.shared.cs.storage.label.ObjectStorageLabel;
import rocks.inspectit.shared.cs.storage.label.StringStorageLabel;
import rocks.inspectit.shared.cs.storage.label.management.impl.AddLabelManagementAction;
import rocks.inspectit.shared.cs.storage.label.management.impl.RemoveLabelManagementAction;
import rocks.inspectit.shared.cs.storage.label.type.impl.AssigneeLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.CreationDateLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.CustomBooleanLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.CustomDateLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.CustomNumberLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.CustomStringLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.DataTimeFrameLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.ExploredByLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.RatingLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.StatusLabelType;
import rocks.inspectit.shared.cs.storage.label.type.impl.UseCaseLabelType;
import rocks.inspectit.shared.cs.storage.processor.impl.AgentFilterDataProcessor;
import rocks.inspectit.shared.cs.storage.processor.impl.DataAggregatorProcessor;
import rocks.inspectit.shared.cs.storage.processor.impl.DataSaverProcessor;
import rocks.inspectit.shared.cs.storage.processor.impl.InvocationClonerDataProcessor;
import rocks.inspectit.shared.cs.storage.processor.impl.InvocationExtractorDataProcessor;
import rocks.inspectit.shared.cs.storage.processor.impl.TimeFrameDataProcessor;
import rocks.inspectit.shared.cs.storage.recording.RecordingProperties;
import rocks.inspectit.shared.cs.storage.recording.RecordingState;
import rocks.inspectit.shared.cs.storage.serializer.impl.ServerStatusSerializer;
import rocks.inspectit.shared.cs.tracing.comparator.SpanComparator;

/**
 * Registers all classes from the CommonsCS project after {@link SerializationManager} has been
 * created.
 *
 * @author Ivan Senic
 *
 */
@Component
public class SerializationManagerPostProcessor implements BeanPostProcessor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof SerializationManager) {
			registerClasses((SerializationManager) bean);
		}
		return bean;
	}

	/**
	 * Registers all classes in the CommonsCS project that needed to be registered to any
	 * {@link SerializationManager} instance.
	 *
	 * @param serializationManager
	 *            {@link SerializationManager}.
	 */
	private void registerClasses(SerializationManager serializationManager) { // NOCHK
		/**
		 * To be able to keep the compatibility, we need to register classes with the same ID. Since
		 * the {@link SerializationManager} will perform registration of classes in the CommonsCS
		 * project, we need to make sure that the registration in this processor starts from the far
		 * away ID so that no overlapping can occur if the new classes are registered in the
		 * original {@link SerializationManager}.
		 */
		int nextRegistrationId = 512;

		Kryo kryo = serializationManager.getKryo();
		ClassSchemaManager schemaManager = serializationManager.getSchemaManager();

		/** Arrays */
		kryo.register(SimpleStorageDescriptor[].class, new ObjectArraySerializer(kryo, SimpleStorageDescriptor[].class), nextRegistrationId++);
		/** Storage classes */
		kryo.register(StorageBranch.class, new CustomCompatibleFieldSerializer<StorageBranch<?>>(kryo, StorageBranch.class, schemaManager), nextRegistrationId++);
		kryo.register(StorageBranchIndexer.class, new CustomCompatibleFieldSerializer<StorageBranchIndexer<?>>(kryo, StorageBranchIndexer.class, schemaManager), nextRegistrationId++);
		kryo.register(SimpleStorageDescriptor.class, new CustomCompatibleFieldSerializer<SimpleStorageDescriptor>(kryo, SimpleStorageDescriptor.class, schemaManager), nextRegistrationId++);
		// we must not copy transient fields of leaf serializer (read/write locks)
		CustomCompatibleFieldSerializer<ArrayBasedStorageLeaf<?>> leafSerializer = new CustomCompatibleFieldSerializer<>(kryo, ArrayBasedStorageLeaf.class, schemaManager);
		leafSerializer.setCopyTransient(false);
		kryo.register(ArrayBasedStorageLeaf.class, leafSerializer, nextRegistrationId++);
		kryo.register(LeafWithNoDescriptors.class, new CustomCompatibleFieldSerializer<LeafWithNoDescriptors<?>>(kryo, LeafWithNoDescriptors.class, schemaManager), nextRegistrationId++);
		kryo.register(StorageData.class, new CustomCompatibleFieldSerializer<StorageData>(kryo, StorageData.class, schemaManager), nextRegistrationId++);
		kryo.register(LocalStorageData.class, new CustomCompatibleFieldSerializer<LocalStorageData>(kryo, LocalStorageData.class, schemaManager), nextRegistrationId++);
		kryo.register(StorageState.class, new EnumSerializer(StorageState.class));
		/** Storage labels */
		kryo.register(BooleanStorageLabel.class, new CustomCompatibleFieldSerializer<BooleanStorageLabel>(kryo, BooleanStorageLabel.class, schemaManager), nextRegistrationId++);
		kryo.register(DateStorageLabel.class, new CustomCompatibleFieldSerializer<DateStorageLabel>(kryo, DateStorageLabel.class, schemaManager), nextRegistrationId++);
		kryo.register(NumberStorageLabel.class, new CustomCompatibleFieldSerializer<NumberStorageLabel>(kryo, NumberStorageLabel.class, schemaManager), nextRegistrationId++);
		kryo.register(StringStorageLabel.class, new CustomCompatibleFieldSerializer<StringStorageLabel>(kryo, StringStorageLabel.class, schemaManager), nextRegistrationId++);
		/** Storage labels type */
		kryo.register(AssigneeLabelType.class, new CustomCompatibleFieldSerializer<AssigneeLabelType>(kryo, AssigneeLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(CreationDateLabelType.class, new CustomCompatibleFieldSerializer<CreationDateLabelType>(kryo, CreationDateLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(CustomBooleanLabelType.class, new CustomCompatibleFieldSerializer<CustomBooleanLabelType>(kryo, CustomBooleanLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(CustomDateLabelType.class, new CustomCompatibleFieldSerializer<CustomDateLabelType>(kryo, CustomDateLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(CustomNumberLabelType.class, new CustomCompatibleFieldSerializer<CustomNumberLabelType>(kryo, CustomNumberLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(CustomStringLabelType.class, new CustomCompatibleFieldSerializer<CustomStringLabelType>(kryo, CustomStringLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(ExploredByLabelType.class, new CustomCompatibleFieldSerializer<ExploredByLabelType>(kryo, ExploredByLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(RatingLabelType.class, new CustomCompatibleFieldSerializer<RatingLabelType>(kryo, RatingLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(StatusLabelType.class, new CustomCompatibleFieldSerializer<StatusLabelType>(kryo, StatusLabelType.class, schemaManager, true), nextRegistrationId++);
		kryo.register(UseCaseLabelType.class, new CustomCompatibleFieldSerializer<UseCaseLabelType>(kryo, UseCaseLabelType.class, schemaManager, true), nextRegistrationId++);
		/** Branch indexers */
		kryo.register(PlatformIdentIndexer.class, new FieldSerializer<PlatformIdentIndexer<?>>(kryo, PlatformIdentIndexer.class), nextRegistrationId++);
		kryo.register(ObjectTypeIndexer.class, new FieldSerializer<ObjectTypeIndexer<?>>(kryo, ObjectTypeIndexer.class), nextRegistrationId++);
		kryo.register(MethodIdentIndexer.class, new FieldSerializer<MethodIdentIndexer<?>>(kryo, MethodIdentIndexer.class), nextRegistrationId++);
		kryo.register(SensorTypeIdentIndexer.class, new FieldSerializer<SensorTypeIdentIndexer<?>>(kryo, SensorTypeIdentIndexer.class), nextRegistrationId++);
		kryo.register(TimestampIndexer.class, new CustomCompatibleFieldSerializer<TimestampIndexer<?>>(kryo, TimestampIndexer.class, schemaManager), nextRegistrationId++);
		kryo.register(InvocationChildrenIndexer.class, new FieldSerializer<InvocationChildrenIndexer<?>>(kryo, InvocationChildrenIndexer.class), nextRegistrationId++);
		kryo.register(SqlStringIndexer.class, new FieldSerializer<SqlStringIndexer<?>>(kryo, SqlStringIndexer.class), nextRegistrationId++);

		// data classes between CMR and UI
		// this classes can be registered with FieldSerializer since they are not saved to disk
		kryo.register(RecordingData.class, new FieldSerializer<RecordingData>(kryo, RecordingData.class), nextRegistrationId++);
		kryo.register(WritingStatus.class, new EnumSerializer(WritingStatus.class), nextRegistrationId++);
		kryo.register(AddLabelManagementAction.class, new FieldSerializer<AddLabelManagementAction>(kryo, AddLabelManagementAction.class), nextRegistrationId++);
		kryo.register(RemoveLabelManagementAction.class, new FieldSerializer<RemoveLabelManagementAction>(kryo, RemoveLabelManagementAction.class), nextRegistrationId++);
		kryo.register(DataAggregatorProcessor.class, new FieldSerializer<DataAggregatorProcessor<?>>(kryo, DataAggregatorProcessor.class), nextRegistrationId++);
		kryo.register(DataSaverProcessor.class, new FieldSerializer<DataSaverProcessor>(kryo, DataSaverProcessor.class), nextRegistrationId++);
		kryo.register(InvocationClonerDataProcessor.class, new FieldSerializer<InvocationClonerDataProcessor>(kryo, InvocationClonerDataProcessor.class), nextRegistrationId++);
		kryo.register(InvocationExtractorDataProcessor.class, new FieldSerializer<InvocationExtractorDataProcessor>(kryo, InvocationExtractorDataProcessor.class), nextRegistrationId++);
		kryo.register(TimeFrameDataProcessor.class, new FieldSerializer<TimeFrameDataProcessor>(kryo, TimeFrameDataProcessor.class), nextRegistrationId++);
		kryo.register(TimerDataAggregator.class, new FieldSerializer<TimerDataAggregator>(kryo, TimerDataAggregator.class), nextRegistrationId++);
		kryo.register(SqlStatementDataAggregator.class, new FieldSerializer<SqlStatementDataAggregator>(kryo, SqlStatementDataAggregator.class), nextRegistrationId++);
		kryo.register(HttpTimerDataAggregator.class, new FieldSerializer<HttpTimerDataAggregator>(kryo, HttpTimerDataAggregator.class), nextRegistrationId++);
		kryo.register(ExceptionDataAggregator.class, new FieldSerializer<ExceptionDataAggregator>(kryo, ExceptionDataAggregator.class), nextRegistrationId++);

		// added with INSPECTIT-723
		kryo.register(RecordingState.class, new EnumSerializer(RecordingState.class), nextRegistrationId++);
		kryo.register(RecordingProperties.class, new FieldSerializer<RecordingProperties>(kryo, RecordingProperties.class), nextRegistrationId++);

		// added with INSPECTIT-937
		kryo.register(AgentFilterDataProcessor.class, new FieldSerializer<AgentFilterDataProcessor>(kryo, AgentFilterDataProcessor.class), nextRegistrationId++);

		// added with INSPECTIT-950
		kryo.register(ObjectStorageLabel.class, new CustomCompatibleFieldSerializer<ObjectStorageLabel<?>>(kryo, ObjectStorageLabel.class, schemaManager), nextRegistrationId++);
		kryo.register(DataTimeFrameLabelType.class, new CustomCompatibleFieldSerializer<DataTimeFrameLabelType>(kryo, DataTimeFrameLabelType.class, schemaManager, true), nextRegistrationId++);

		// added with INSPECTIT-991
		kryo.register(ServerStatus.class, new ServerStatusSerializer(), nextRegistrationId++);

		// added with INSPECTIT-963
		// CMR Configuration Properties classes
		// this classes can be registered with FieldSerializer since they are not saved to disk
		kryo.register(GroupedProperty.class, new FieldSerializer<GroupedProperty>(kryo, GroupedProperty.class), nextRegistrationId++);
		kryo.register(BooleanProperty.class, new FieldSerializer<BooleanProperty>(kryo, BooleanProperty.class), nextRegistrationId++);
		kryo.register(BooleanPropertyUpdate.class, new FieldSerializer<BooleanPropertyUpdate>(kryo, BooleanPropertyUpdate.class), nextRegistrationId++);
		kryo.register(LongProperty.class, new FieldSerializer<LongProperty>(kryo, LongProperty.class), nextRegistrationId++);
		kryo.register(LongPropertyUpdate.class, new FieldSerializer<LongPropertyUpdate>(kryo, LongPropertyUpdate.class), nextRegistrationId++);
		kryo.register(PercentageProperty.class, new FieldSerializer<PercentageProperty>(kryo, PercentageProperty.class), nextRegistrationId++);
		kryo.register(PercentagePropertyUpdate.class, new FieldSerializer<PercentagePropertyUpdate>(kryo, PercentagePropertyUpdate.class), nextRegistrationId++);
		kryo.register(ByteProperty.class, new FieldSerializer<ByteProperty>(kryo, ByteProperty.class), nextRegistrationId++);
		kryo.register(BytePropertyUpdate.class, new FieldSerializer<BytePropertyUpdate>(kryo, BytePropertyUpdate.class), nextRegistrationId++);
		kryo.register(StringProperty.class, new FieldSerializer<StringProperty>(kryo, StringProperty.class), nextRegistrationId++);
		kryo.register(StringPropertyUpdate.class, new FieldSerializer<StringPropertyUpdate>(kryo, StringPropertyUpdate.class), nextRegistrationId++);
		kryo.register(RestoreDefaultPropertyUpdate.class, new FieldSerializer<RestoreDefaultPropertyUpdate<?>>(kryo, RestoreDefaultPropertyUpdate.class), nextRegistrationId++);
		kryo.register(Configuration.class, new FieldSerializer<Configuration>(kryo, Configuration.class), nextRegistrationId++);
		kryo.register(ConfigurationUpdate.class, new FieldSerializer<ConfigurationUpdate>(kryo, ConfigurationUpdate.class), nextRegistrationId++);
		kryo.register(PropertySection.class, new FieldSerializer<PropertySection>(kryo, PropertySection.class), nextRegistrationId++);
		// validations
		kryo.register(PropertyValidation.class, new FieldSerializer<PropertyValidation>(kryo, PropertyValidation.class), nextRegistrationId++);
		kryo.register(ValidationError.class, new FieldSerializer<ValidationError>(kryo, ValidationError.class), nextRegistrationId++);
		kryo.register(PropertyValidationException.class, new FieldSerializer<PropertyValidationException>(kryo, PropertyValidationException.class), nextRegistrationId++);
		// validators
		kryo.register(FullyQualifiedClassNameValidator.class, new FieldSerializer<FullyQualifiedClassNameValidator>(kryo, FullyQualifiedClassNameValidator.class), nextRegistrationId++);
		kryo.register(GreaterOrEqualValidator.class, new FieldSerializer<GreaterOrEqualValidator<?>>(kryo, GreaterOrEqualValidator.class), nextRegistrationId++);
		kryo.register(GreaterValidator.class, new FieldSerializer<GreaterValidator<?>>(kryo, GreaterValidator.class), nextRegistrationId++);
		kryo.register(LessOrEqualValidator.class, new FieldSerializer<LessOrEqualValidator<?>>(kryo, LessOrEqualValidator.class), nextRegistrationId++);
		kryo.register(LessValidator.class, new FieldSerializer<LessValidator<?>>(kryo, LessValidator.class), nextRegistrationId++);
		kryo.register(NegativeValidator.class, new FieldSerializer<NegativeValidator<?>>(kryo, NegativeValidator.class), nextRegistrationId++);
		kryo.register(NotEmptyValidator.class, new FieldSerializer<NotEmptyValidator<?>>(kryo, NotEmptyValidator.class), nextRegistrationId++);
		kryo.register(PercentageValidator.class, new FieldSerializer<PercentageValidator<?>>(kryo, PercentageValidator.class), nextRegistrationId++);
		kryo.register(PositiveValidator.class, new FieldSerializer<PositiveValidator<?>>(kryo, PositiveValidator.class), nextRegistrationId++);

		// added with INSPECTIT-1804
		// used for recognition, configuration and visualization of business context information
		kryo.register(ApplicationDefinition.class, new FieldSerializer<ApplicationDefinition>(kryo, ApplicationDefinition.class), nextRegistrationId++);
		kryo.register(BusinessContextDefinition.class, new FieldSerializer<BusinessContextDefinition>(kryo, BusinessContextDefinition.class), nextRegistrationId++);
		kryo.register(BusinessTransactionDefinition.class, new FieldSerializer<BusinessTransactionDefinition>(kryo, BusinessTransactionDefinition.class), nextRegistrationId++);
		kryo.register(AbstractExpression.class, new FieldSerializer<AbstractExpression>(kryo, AbstractExpression.class), nextRegistrationId++);
		kryo.register(AndExpression.class, new FieldSerializer<AndExpression>(kryo, AndExpression.class), nextRegistrationId++);
		kryo.register(NotExpression.class, new FieldSerializer<NotExpression>(kryo, NotExpression.class), nextRegistrationId++);
		kryo.register(OrExpression.class, new FieldSerializer<OrExpression>(kryo, OrExpression.class), nextRegistrationId++);
		kryo.register(BooleanExpression.class, new FieldSerializer<BooleanExpression>(kryo, BooleanExpression.class), nextRegistrationId++);
		kryo.register(StringMatchingExpression.class, new FieldSerializer<StringMatchingExpression>(kryo, StringMatchingExpression.class), nextRegistrationId++);
		kryo.register(PatternMatchingType.class, new EnumSerializer(PatternMatchingType.class));
		kryo.register(StringValueSource.class, new FieldSerializer<StringValueSource>(kryo, StringValueSource.class), nextRegistrationId++);
		kryo.register(HttpUriValueSource.class, new FieldSerializer<HttpUriValueSource>(kryo, HttpUriValueSource.class), nextRegistrationId++);
		kryo.register(HostValueSource.class, new FieldSerializer<HostValueSource>(kryo, HostValueSource.class), nextRegistrationId++);
		kryo.register(HttpParameterValueSource.class, new FieldSerializer<HttpParameterValueSource>(kryo, HttpParameterValueSource.class), nextRegistrationId++);
		kryo.register(MethodSignatureValueSource.class, new FieldSerializer<MethodSignatureValueSource>(kryo, MethodSignatureValueSource.class), nextRegistrationId++);
		kryo.register(MethodParameterValueSource.class, new FieldSerializer<MethodParameterValueSource>(kryo, MethodParameterValueSource.class), nextRegistrationId++);
		kryo.register(NameExtractionExpression.class, new FieldSerializer<NameExtractionExpression>(kryo, NameExtractionExpression.class), nextRegistrationId++);
		kryo.register(HttpRequestMethodValueSource.class, new FieldSerializer<HttpRequestMethodValueSource>(kryo, HttpRequestMethodValueSource.class), nextRegistrationId++);
		// INSPECTIT-2302
		kryo.register(HttpUrlValueSource.class, new FieldSerializer<HttpUrlValueSource>(kryo, HttpUrlValueSource.class), nextRegistrationId++);
		kryo.register(HttpSchemeValueSource.class, new FieldSerializer<HttpSchemeValueSource>(kryo, HttpSchemeValueSource.class), nextRegistrationId++);
		kryo.register(HttpServerNameValueSource.class, new FieldSerializer<HttpServerNameValueSource>(kryo, HttpServerNameValueSource.class), nextRegistrationId++);
		kryo.register(HttpServerPortValueSource.class, new FieldSerializer<HttpServerPortValueSource>(kryo, HttpServerPortValueSource.class), nextRegistrationId++);
		kryo.register(HttpQueryStringValueSource.class, new FieldSerializer<HttpQueryStringValueSource>(kryo, HttpQueryStringValueSource.class), nextRegistrationId++);
		// INSPECTIT-658
		// this classes are registered with CompatibleFieldSerializer since they can be
		// exported/imported
		kryo.register(AgentMapping.class, new FieldSerializer<AgentMapping>(kryo, AgentMapping.class), nextRegistrationId++);
		kryo.register(AgentMappings.class, new FieldSerializer<AgentMappings>(kryo, AgentMappings.class), nextRegistrationId++);
		kryo.register(Environment.class, new FieldSerializer<Environment>(kryo, Environment.class), nextRegistrationId++);
		kryo.register(Profile.class, new FieldSerializer<Profile>(kryo, Profile.class), nextRegistrationId++);
		// assignments
		kryo.register(ExceptionSensorAssignment.class, new FieldSerializer<ExceptionSensorAssignment>(kryo, ExceptionSensorAssignment.class), nextRegistrationId++);
		kryo.register(MethodSensorAssignment.class, new FieldSerializer<MethodSensorAssignment>(kryo, MethodSensorAssignment.class), nextRegistrationId++);
		kryo.register(TimerMethodSensorAssignment.class, new FieldSerializer<TimerMethodSensorAssignment>(kryo, TimerMethodSensorAssignment.class), nextRegistrationId++);
		// context capture
		kryo.register(FieldContextCapture.class, new FieldSerializer<FieldContextCapture>(kryo, FieldContextCapture.class), nextRegistrationId++);
		kryo.register(ParameterContextCapture.class, new FieldSerializer<ParameterContextCapture>(kryo, ParameterContextCapture.class), nextRegistrationId++);
		kryo.register(ReturnContextCapture.class, new FieldSerializer<ReturnContextCapture>(kryo, ReturnContextCapture.class), nextRegistrationId++);
		// exclude
		kryo.register(ExcludeRule.class, new FieldSerializer<ExcludeRule>(kryo, ExcludeRule.class), nextRegistrationId++);
		// exception sensor config
		kryo.register(ExceptionSensorConfig.class, new FieldSerializer<ExceptionSensorConfig>(kryo, ExceptionSensorConfig.class), nextRegistrationId++);
		// method sensor configs
		kryo.register(ConnectionSensorConfig.class, new FieldSerializer<ConnectionSensorConfig>(kryo, ConnectionSensorConfig.class), nextRegistrationId++);
		kryo.register(HttpSensorConfig.class, new FieldSerializer<HttpSensorConfig>(kryo, HttpSensorConfig.class), nextRegistrationId++);
		kryo.register(InvocationSequenceSensorConfig.class, new FieldSerializer<InvocationSequenceSensorConfig>(kryo, InvocationSequenceSensorConfig.class), nextRegistrationId++);
		kryo.register(PreparedStatementParameterSensorConfig.class, new FieldSerializer<PreparedStatementParameterSensorConfig>(kryo, PreparedStatementParameterSensorConfig.class), nextRegistrationId++);
		kryo.register(PreparedStatementSensorConfig.class, new FieldSerializer<PreparedStatementSensorConfig>(kryo, PreparedStatementSensorConfig.class), nextRegistrationId++);
		kryo.register(StatementSensorConfig.class, new FieldSerializer<StatementSensorConfig>(kryo, StatementSensorConfig.class), nextRegistrationId++);
		kryo.register(TimerSensorConfig.class, new FieldSerializer<TimerSensorConfig>(kryo, TimerSensorConfig.class), nextRegistrationId++);
		// platform sensor configs
		kryo.register(ClassLoadingSensorConfig.class, new FieldSerializer<ClassLoadingSensorConfig>(kryo, ClassLoadingSensorConfig.class), nextRegistrationId++);
		kryo.register(CompilationSensorConfig.class, new FieldSerializer<CompilationSensorConfig>(kryo, CompilationSensorConfig.class), nextRegistrationId++);
		kryo.register(CpuSensorConfig.class, new FieldSerializer<CpuSensorConfig>(kryo, CpuSensorConfig.class), nextRegistrationId++);
		kryo.register(MemorySensorConfig.class, new FieldSerializer<MemorySensorConfig>(kryo, MemorySensorConfig.class), nextRegistrationId++);
		kryo.register(RuntimeSensorConfig.class, new FieldSerializer<RuntimeSensorConfig>(kryo, RuntimeSensorConfig.class), nextRegistrationId++);
		kryo.register(SystemSensorConfig.class, new FieldSerializer<SystemSensorConfig>(kryo, SystemSensorConfig.class), nextRegistrationId++);
		kryo.register(ThreadSensorConfig.class, new FieldSerializer<ThreadSensorConfig>(kryo, ThreadSensorConfig.class), nextRegistrationId++);
		// 4 strategies removed, 1 added
		nextRegistrationId++;
		nextRegistrationId++;
		nextRegistrationId++;
		kryo.register(DisruptorStrategyConfig.class, new FieldSerializer<>(kryo, DisruptorStrategyConfig.class), nextRegistrationId++);

		// INSPECTIT-2020
		kryo.register(Log4jLoggingSensorConfig.class, new FieldSerializer<Log4jLoggingSensorConfig>(kryo, Log4jLoggingSensorConfig.class), nextRegistrationId++);

		// INSPECTIT-2021
		kryo.register(JmxBeanSensorAssignment.class, new FieldSerializer<JmxBeanSensorAssignment>(kryo, JmxBeanSensorAssignment.class), nextRegistrationId++);
		kryo.register(SensorAssignmentProfileData.class, new FieldSerializer<SensorAssignmentProfileData>(kryo, SensorAssignmentProfileData.class), nextRegistrationId++);
		kryo.register(ExcludeRulesProfileData.class, new FieldSerializer<ExcludeRulesProfileData>(kryo, ExcludeRulesProfileData.class), nextRegistrationId++);
		kryo.register(JmxDefinitionProfileData.class, new FieldSerializer<JmxDefinitionProfileData>(kryo, JmxDefinitionProfileData.class), nextRegistrationId++);

		// INSPECTIT-2101
		kryo.register(ChartingMethodSensorAssignment.class, new FieldSerializer<ChartingMethodSensorAssignment>(kryo, ChartingMethodSensorAssignment.class), nextRegistrationId++);

		// INSPECTIT-2031
		kryo.register(ConfigurationInterfaceImportData.class, new FieldSerializer<>(kryo, ConfigurationInterfaceImportData.class), nextRegistrationId++);

		// INSPECTIT-2071
		kryo.register(JmxSensorConfig.class, new FieldSerializer<JmxSensorConfig>(kryo, JmxSensorConfig.class), nextRegistrationId++);

		// INSPECTIT-1953
		kryo.register(AlertingDefinition.class, new FieldSerializer<AlertingDefinition>(kryo, AlertingDefinition.class), nextRegistrationId++);
		kryo.register(Alert.class, new FieldSerializer<Alert>(kryo, Alert.class), nextRegistrationId++);
		kryo.register(EMailValidator.class, new FieldSerializer<EMailValidator>(kryo, EMailValidator.class), nextRegistrationId++);
		kryo.register(EMailListValidator.class, new FieldSerializer<EMailListValidator>(kryo, EMailListValidator.class), nextRegistrationId++);
		kryo.register(AlertClosingReason.class, new EnumSerializer(AlertClosingReason.class), nextRegistrationId++);

		// INSPECTIT-1921
		kryo.register(RemoteApacheHttpClientV40SensorConfig.class, new FieldSerializer<>(kryo, RemoteApacheHttpClientV40SensorConfig.class), nextRegistrationId++);
		kryo.register(RemoteJettyHttpClientV61ClientSensorConfig.class, new FieldSerializer<>(kryo, RemoteJettyHttpClientV61ClientSensorConfig.class), nextRegistrationId++);
		kryo.register(RemoteUrlConnectionClientSensorConfig.class, new FieldSerializer<>(kryo, RemoteUrlConnectionClientSensorConfig.class), nextRegistrationId++);
		kryo.register(RemoteSpringRestTemplateClientSensorConfig.class, new FieldSerializer<>(kryo, RemoteSpringRestTemplateClientSensorConfig.class), nextRegistrationId++);
		kryo.register(RemoteJavaHttpServerSensorConfig.class, new FieldSerializer<>(kryo, RemoteJavaHttpServerSensorConfig.class), nextRegistrationId++);
		kryo.register(RemoteJmsClientSensorConfig.class, new FieldSerializer<>(kryo, RemoteJmsClientSensorConfig.class), nextRegistrationId++);
		kryo.register(RemoteJmsListenerServerSensorConfig.class, new FieldSerializer<>(kryo, RemoteJmsListenerServerSensorConfig.class), nextRegistrationId++);
		kryo.register(RemoteManualServerSensorConfig.class, new FieldSerializer<>(kryo, RemoteManualServerSensorConfig.class), nextRegistrationId++);

		// moved from Serialization manager with INSPECTIT-2276
		kryo.register(DefaultDataComparatorEnum.class, new EnumSerializer(DefaultDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(MethodSensorDataComparatorEnum.class, new EnumSerializer(MethodSensorDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(InvocationAwareDataComparatorEnum.class, new EnumSerializer(InvocationAwareDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(TimerDataComparatorEnum.class, new EnumSerializer(TimerDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(HttpTimerDataComparatorEnum.class, new EnumSerializer(HttpTimerDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(SqlStatementDataComparatorEnum.class, new EnumSerializer(SqlStatementDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(ExceptionSensorDataComparatorEnum.class, new EnumSerializer(ExceptionSensorDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(AggregatedExceptionSensorDataComparatorEnum.class, new EnumSerializer(AggregatedExceptionSensorDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(InvocationSequenceDataComparatorEnum.class, new EnumSerializer(InvocationSequenceDataComparatorEnum.class), nextRegistrationId++);
		kryo.register(ResultComparator.class, new FieldSerializer<ResultComparator<?>>(kryo, ResultComparator.class), nextRegistrationId++);
		kryo.register(SpanComparator.class, new EnumSerializer(SpanComparator.class), nextRegistrationId++);

		// INSPECTIT-1959
		kryo.register(EndUserMonitoringConfig.class, new FieldSerializer<EndUserMonitoringConfig>(kryo, EndUserMonitoringConfig.class), nextRegistrationId++);

		// INSPECTIT-2467
		kryo.register(InvocationStartMethodSensorAssignment.class, new FieldSerializer<>(kryo, InvocationStartMethodSensorAssignment.class), nextRegistrationId++);

		// INSPECTIT-2432
		kryo.register(ExecutorIntercepterSensorConfig.class, new FieldSerializer<>(kryo, ExecutorIntercepterSensorConfig.class), nextRegistrationId++);
		kryo.register(ExecutorClientSensorConfig.class, new FieldSerializer<>(kryo, ExecutorClientSensorConfig.class), nextRegistrationId++);

		// INSPECTIT-2192
		kryo.register(AgentNameValueSource.class, new FieldSerializer<AgentNameValueSource>(kryo, AgentNameValueSource.class), nextRegistrationId++);

		// INSPECTIT-2458
		kryo.register(RemoteAsyncApacheHttpClientSensorConfig.class, new FieldSerializer<>(kryo, RemoteAsyncApacheHttpClientSensorConfig.class), nextRegistrationId++);
		kryo.register(ApacheClientExchangeHandlerSensorConfig.class, new FieldSerializer<>(kryo, ApacheClientExchangeHandlerSensorConfig.class), nextRegistrationId++);
	}

}
