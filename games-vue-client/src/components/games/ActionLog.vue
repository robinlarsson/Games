<template>
  <v-card v-show="logEntries.length > 0">
    <v-card-title>
      <h2>{{ title }}</h2>
    </v-card-title>
    <v-list :class="{ reversed: reversed }">
      <v-list-item
        v-for="(entry, index) in logEntries"
        :key="index"
      >
        <v-list-item-avatar>
          <!--                    <PlayerProfile /> -->
        </v-list-item-avatar>
        <v-list-item-content>
          <v-list-item-title />
          <v-list-item-subtitle>
            <component
              :is="components[part.type].component"
              v-for="(part, partIndex) in entry.parts"
              :key="partIndex"
              :private="entry.private"
              v-bind="components[part.type].binds(part)"
            />
          </v-list-item-subtitle>
        </v-list-item-content>
      </v-list-item>
    </v-list>
  </v-card>
</template>
<script>
import supportedGames from "@/supportedGames"
import PlayerProfile from "@/components/games/common/PlayerProfile"
import LogEntryText from "@/components/action-log/LogEntryText"

export default {
    name: "ActionLog",
    props: {
      logEntries: Array,
      context: Object,
      title: { type: String, default: "Action Log" },
      reversed: { type: Boolean, default: true }
    },
    components: { PlayerProfile },
    methods: {
        highlight(value) {
            console.log("highlight", value)
        }
    },
    computed: {
        supportedGame() {
            return supportedGames.games[this.context.gameType]
        },
        components() {
            return {
                player: {
                    component: PlayerProfile,
                    binds: (part) => ({ context: this.context, playerIndex: part.playerIndex })
                },
                text: {
                    component: LogEntryText,
                    binds: (part) => ({ text: part.text })
                },
                link: {
                    component: LogEntryText,
                    binds: (part) => ({
                        text: part.text,
                        tooltipComponent: this.supportedGame.viewTypes[part.viewType].component,
                        hoverBindings: this.supportedGame.viewTypes[part.viewType].binds(part.value)
                    })
                },
                highlight: {
                    component: LogEntryText,
                    binds: (part) => ({ text: part.value, onHighlight: this.highlight })
                },
            }
        }
    }
}
</script>
<style scoped>
.v-list {
    display: flex;
    flex-direction: column;
}
.v-list.reversed {
    display: flex;
    flex-direction: column-reverse;
}
</style>
