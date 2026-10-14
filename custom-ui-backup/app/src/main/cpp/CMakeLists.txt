cmake_minimum_required(VERSION 3.22.1)
project(bedrock_runtime_bridge LANGUAGES CXX)

add_library(bedrock_runtime_bridge SHARED native_runtime_bridge.cpp)
target_compile_features(bedrock_runtime_bridge PRIVATE cxx_std_17)
target_compile_options(bedrock_runtime_bridge PRIVATE -Wall -Wextra -Werror)
