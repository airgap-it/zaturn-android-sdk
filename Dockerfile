FROM androidsdk/android-29:latest

RUN mkdir /build
WORKDIR /build

# copy source
COPY . /build

# accept licences
#RUN echo y | $ANDROID_HOME/tools/bin/sdkmanager --update

# clean project
RUN /build/gradlew --project-dir /build clean

# build apk, exclude prod flavored unit tests
RUN /build/gradlew --project-dir /build :sdk:assemble

# copy release aar
RUN cp /build/sdk/build/outputs/aar/sdk-release.aar android-sdk-release-unsigned.aar

CMD ["/build/gradlew", "test"]
