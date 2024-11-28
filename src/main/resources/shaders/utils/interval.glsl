bool interval_surrounds(Interval interval, float x) {
    return interval.min < x && x < interval.max;
}

bool interval_contains(Interval interval, float x) {
    return interval.min <= x && x <= interval.max;
}