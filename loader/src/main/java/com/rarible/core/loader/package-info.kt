/**
 * Library for scheduling background loaders (tasks).
 *
 * Loaders can fail and the library guarantees to retry them a configured number of times
 * with configured delays.
 *
 * The tasks are guaranteed to be executed at least once, BUT MAY BE EXECUTED SEVERAL times,
 * if an unlucky coincidence of failures/restarts of workers happen.
 *
 * TODO: decouple schedulers and workers services.
 */
package com.rarible.core.loader
