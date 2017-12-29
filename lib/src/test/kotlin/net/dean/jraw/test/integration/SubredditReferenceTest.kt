package net.dean.jraw.test.integration

import com.winterbe.expekt.should
import net.dean.jraw.ApiException
import net.dean.jraw.models.SimpleFlairInfo
import net.dean.jraw.models.SubmissionKind
import net.dean.jraw.test.SharedObjects
import net.dean.jraw.test.TestConfig.reddit
import net.dean.jraw.test.TestConfig.redditUserless
import net.dean.jraw.test.assume
import net.dean.jraw.test.expectException
import net.dean.jraw.test.ignoreRateLimit
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.*

class SubredditReferenceTest : Spek({
    describe("submit") {
        val now = Date()
        val ref = reddit.subreddit("jraw_testing2")

        // submittedSelfPost is a lazily-initiated object that is created by attempting to submit a self post. All we
        // have to do is access it.
        assume({ SharedObjects.submittedSelfPost != null }, description = "should be able to submit a self post") {}

        it("should be able to submit a link") {
            ignoreRateLimit {
                val postRef = ref.submit(SubmissionKind.LINK, "test link post", "http://example.com/${now.time}", sendReplies = false)
                postRef.inspect().subreddit.should.equal(ref.subreddit)
            }
        }
    }

    describe("subscribe/unsubscribe") {
        it("should subscribe the user to the specific subreddit") {
            val pics = reddit.subreddit("pics")
            pics.subscribe()
            pics.about().isUserSubscriber.should.be.`true`
            pics.unsubscribe()
            pics.about().isUserSubscriber.should.be.`false`
        }
    }

    describe("submitText") {
        it("should return a string") {
            reddit.subreddit("pics").submitText().should.have.length.above(0)
        }
    }

    describe("userFlairOptions/linkFlairOptions") {
        val srName = "jraw_testing2"

        it("should throw an ApiException when there is no active user") {
            expectException(ApiException::class) {
                redditUserless.subreddit(srName).linkFlairOptions()
            }

            expectException(ApiException::class) {
                redditUserless.subreddit(srName).userFlairOptions()
            }
        }

        it("should return a list of Flairs") {
            reddit.subreddit(srName).linkFlairOptions().should.have.size.above(0)
            reddit.subreddit(srName).userFlairOptions().should.have.size.above(0)
        }
    }

    describe("rules") {
        it("should return a Ruleset") {
            val rules = reddit.subreddit("jraw_testing2").rules()
            rules.subredditRules.should.have.size.above(0)
            rules.siteRules.should.have.size.above(0)
        }
    }

    describe("getStylesheet") {
        it("should return text") {
            reddit.subreddit("RocketLeague").stylesheet().should.have.length.above(0)
        }
    }

    val moddedSubreddit = reddit.subreddit("jraw_testing2")
    describe("updateStylesheet") {
        val newStylesheet = "#test${Random().nextInt()}{color:red}"

        it("should update the modded subreddit stylesheet") {
            moddedSubreddit.updateStylesheet(newStylesheet, "JRAW integration test")
            moddedSubreddit.stylesheet().should.equal(newStylesheet)
        }
    }

    describe("flairList") {
        it("should be able to access modded subreddit flair list") {
            moddedSubreddit.flairList().build().accumulateMerged(-1).toList()
        }
    }

    fun randomString() = "test${Random().nextInt()}"
    val flairList = listOf(
        SimpleFlairInfo.create("_vargas_", randomString(), randomString()),
        SimpleFlairInfo.create(reddit.me().username, randomString(), randomString())
    )
    describe("patchFlairList") {
        it("should update the modded subreddit flair list") {
            moddedSubreddit.patchFlairList(flairList)
        }
        it("should have an effect on subreddit flair list") {
            val updatedFlairList = moddedSubreddit.flairList().build().accumulateMerged(-1)

            for(simpleFlairInfo in flairList)
                updatedFlairList.should.contain(simpleFlairInfo)
        }
    }
})
