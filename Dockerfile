FROM amazonlinux:2.0.20211005.0 AS packer

# Add the Amazon Corretto repository
RUN rpm --import https://yum.corretto.aws/corretto.key
RUN curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo

# Update the packages and install Amazon Corretto 17, Maven and Zip
RUN yum -y update \
    && yum install -y java-17-amazon-corretto-jmods maven zip


# Create a slim Java 17 JRE which only contains the required modules to run this function
RUN jlink --add-modules ALL-MODULE-PATH \
    --verbose \
    --compress 2 \
    --strip-java-debug-attributes \
    --no-header-files \
    --no-man-pages \
    --output /jre-17


# Use Javas Application Class Data Sharing feature to precompile JDK and our function.jar file
# it creates the file /jre-17/lib/server/classes.jsa
RUN /jre-17/bin/java -Xshare:dump -version


# Package everything together into a custom runtime archive
WORKDIR /
COPY bootstrap .
RUN zip -r jre-17-layer.zip bootstrap /jre-17
