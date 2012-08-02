.SUFFIXES: .class .java

SRC_DIR=src/main/java
SRC_GEN_DIR=target/generated-sources/$(SRC_DIR)
CLS_DIR=target/classes

CLASS_FILES = \
		$(CLS_DIR)/libsvm/svm.class \
		$(CLS_DIR)/libsvm/svm_model.class \
		$(CLS_DIR)/libsvm/svm_node.class \
		$(CLS_DIR)/libsvm/svm_parameter.class \
		$(CLS_DIR)/libsvm/svm_problem.class \
		$(CLS_DIR)/libsvm/svm_print_interface.class \
		$(CLS_DIR)/svm_train.class \
		$(CLS_DIR)/svm_predict.class \
		$(CLS_DIR)/svm_toy.class \
		$(CLS_DIR)/svm_scale.class

#JAVAC = jikes
JAVAC = javac
JAVAC_FLAGS = -target 1.5 -source 1.5 -sourcepath $(SRC_DIR):$(SRC_GEN_DIR) -d $(CLS_DIR) -cp $(CLS_DIR)

# package the final JAR
all: $(CLASS_FILES)
	jar cvf target/libsvm.jar -C $(CLS_DIR) .

# create output directories
prepare:
	mkdir -p $(CLS_DIR)
	mkdir -p $(SRC_GEN_DIR)/libsvm

# generate sources
$(SRC_GEN_DIR)/libsvm/svm.java: prepare $(SRC_DIR)/libsvm/svm.m4
	m4 $(SRC_DIR)/libsvm/svm.m4 > $(SRC_GEN_DIR)/libsvm/svm.java

# compile sources
$(CLS_DIR)/%.class: $(SRC_DIR)/%.java
	$(JAVAC) $(JAVAC_FLAGS) $<

# compile generated sources
$(CLS_DIR)/%.class: $(SRC_GEN_DIR)/%.java
	$(JAVAC) $(JAVAC_FLAGS) $<

clean:
	rm -Rf \
			target \
			$(SRC_DIR)/libsvm/*~ \
			$(SRC_DIR)/*~

dist: clean all
