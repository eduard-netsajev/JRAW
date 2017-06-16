package net.dean.jraw

/**
 * Shows that a function sends a request to an API endpoint or creates a [Reference][net.dean.jraw.references.Reference]
 * that is directly responsible for sending requests.
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
annotation class EndpointImplementation(val endpoints: Array<Endpoint>)
