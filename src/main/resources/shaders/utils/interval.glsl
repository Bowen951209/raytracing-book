struct Interval {
    float min;
    float max;
};

bool interval_surrounds(Interval interval, float x) {
    return interval.min < x && x < interval.max;
}