#include <jni.h>
#include <string>
#include <sstream>

#include <iostream>
#include <vector>

#include "DBoW2/DBoW2.h" //defines OrbVocabulary and OrbDatabase
// OpenCV
#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/features2d.hpp>




using namespace DBoW2;
using namespace std;

void loadFeatures(vector<vector<cv::Mat > > &features);
void changeStructure(const cv::Mat &plain, vector<cv::Mat> &out);
void testVocCreation(const vector<vector<cv::Mat > > &features);
void testDatabase(const vector<vector<cv::Mat > > &features);

void changeStructure(const cv::Mat &plain, vector<cv::Mat> &out)
{
    out.resize(plain.rows);

    for(int i = 0; i < plain.rows; ++i)
    {
        out[i] = plain.row(i);
    }
}


extern "C" JNIEXPORT jstring
Java_com_example_cppnguide_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    std::string hello = "GG SIR";
    return env->NewStringUTF(hello.c_str());

}
extern "C" JNIEXPORT jstring
Java_com_example_cppnguide_CreateMap_createVocabulary(JNIEnv *env, jobject thiz, int num_images, jstring path) {

    vector<vector<cv::Mat>>features;
    //getting orb in each image
    features.clear();
    features.reserve(num_images);
    cv::Ptr<cv::ORB> orb = cv::ORB::create();
    int count = 0;


    const char *str = env->GetStringUTFChars(path, 0);
    std::string str2 = str;
    std::string filename = str2;

    for(int i = 0; i < num_images; ++i) {
        stringstream ss;
        ss << path << "/Images/" << std::setfill('0') << std::setw(10) << i << ".jpg";
        cv::Mat image = cv::imread(ss.str(), 0);
        cv::Mat mask;
        vector<cv::KeyPoint> keypoints;
        cv::Mat descriptors;
        orb->detectAndCompute(image, mask, keypoints, descriptors);
        features.push_back(vector<cv::Mat>());
        changeStructure(descriptors, features.back());
    }

    // creating Vocabulary
    const int k = 9;
    const int L = 3;
    const WeightingType weight = TF_IDF;
    const ScoringType scoring = L1_NORM;
    OrbVocabulary voc(k, L, weight, scoring);
    voc.create(features);

    voc.save(filename + "/Map/map_voc.yml.gz");

    // creating database
    OrbDatabase db(voc, false, 0);
    // add images to the database
    for(int i = 0; i < num_images; i++)
    {
        db.add(features[i]);
    }
    db.save( filename + "/Map/map_db.yml.gz");
    std::string score = "100";
    return env->NewStringUTF(score.c_str());
}



