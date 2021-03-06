package net.streamok.service.machinelearning

import net.streamok.fiber.node.api.DependencyProvider
import net.streamok.fiber.node.api.Endpoint
import net.streamok.fiber.node.api.OperationDefinition
import net.streamok.fiber.node.api.Service
import net.streamok.service.machinelearning.operation.decision.Decide
import net.streamok.service.machinelearning.operation.decision.TrainDecisionModel
import net.streamok.service.machinelearning.operation.textlabel.LabelAllTextContent
import net.streamok.service.machinelearning.operation.textlabel.LabelTextContent
import net.streamok.service.machinelearning.operation.textlabel.PredictTextLabel
import net.streamok.service.machinelearning.operation.textlabel.TrainTextLabelModel
import net.streamok.service.machinelearning.vertx.ModelCacheProvider
import net.streamok.service.machinelearning.vertx.SparkSessionProvider

class MachineLearningService implements Service {

    @Override
    List<OperationDefinition> operations() {
        [new MachineLearningIngestTrainingData(), new TrainTextLabelModel(), new PredictTextLabel(),
         new LabelTextContent(), new LabelAllTextContent(),
         new TrainDecisionModel(), new Decide()]
    }

    @Override
    List<DependencyProvider> dependencies() {
        [new SparkSessionProvider(), new ModelCacheProvider()]
    }

    @Override
    List<Endpoint> endpoints() {
        []
    }
}