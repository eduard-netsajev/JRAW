package net.dean.jraw.test.integration

import com.winterbe.expekt.should
import net.dean.jraw.RedditClient
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Subreddit
import net.dean.jraw.test.TestConfig
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.reflect.KClass

typealias DeserializeTest = (RedditClient) -> List<Any>

/**
 * We're testing to see if we have our models and Moshi instance set up correctly. We test deserializing both with and
 * without a user because some properties only exist in a specific state.
 */
class DeserializationTest : Spek({
    fun subredditPosts(reddit: RedditClient, sr: String) = reddit.subreddit(sr).posts().sorting(SubredditSort.HOT).build().next()
    // Map a an array of functions that uses Moshi to deserialize JSON into an instance of that class
    val testCases = mapOf<KClass<*>, Array<DeserializeTest>>(
        Subreddit::class to arrayOf<DeserializeTest>(
            // Test both /r/pics and /r/redditdev, two very different subreddits (both content-wise and settings-wise)
            { listOf(it.subreddit("pics").about()) },
            { listOf(it.subreddit("redditdev" ).about()) },
            { listOf(it.randomSubreddit().about()) }
        ),
        Submission::class to arrayOf<DeserializeTest>(
            { subredditPosts(it, "pics") },
            { subredditPosts(it, "redditdev") },
            { subredditPosts(it, "random") }
        )
    )

    // Create a client for each
    val withUser: RedditClient = TestConfig.reddit
    val withoutUser: RedditClient = TestConfig.redditUserless

    // Dynamically create tests for every entry in our testCases map
    for ((klass, testFunctions) in testCases) {
        it("should deserialize a ${klass.java.simpleName} with a logged in user") {
            for (deserializeThing in testFunctions) {
                deserializeThing(withUser)
            }
        }

        it("should deserialize a ${klass.java.simpleName} without a logged in user") {
            for (deserializeThing in testFunctions) {
                deserializeThing(withoutUser)
            }
        }
    }

    describe("Submission media") {
        it("should handle Submissions with OEmbed media") {
            val media = withUser.submission("764nn9").inspect().embeddedMedia
            media.should.not.be.`null`
            media?.type.should.not.equal("liveupdate")
            media?.oEmbed.should.not.be.`null`
        }

        it("should handle Submissions for a live thread") {
            val media = withUser.submission("72dg2e").inspect().embeddedMedia
            media.should.not.be.`null`
            media?.type.should.equal("liveupdate")
            media?.liveThreadId.should.have.length.above(0)
        }

        it("should handle Submissions with no media") {
            withUser.submission("92dd8").inspect().embeddedMedia.should.be.`null`
        }
    }
})
