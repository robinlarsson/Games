<template>
  <div>
    <v-card class="invite-options">
      <v-switch
        v-model="useDatabase"
        label="Use Database"
        disabled
      />
      <v-switch
        v-model="allowAnyoneJoin"
        label="Allow anyone to join"
        disabled
      />
      <v-radio-group
        v-model="playerOrder"
        disabled
      >
        <v-radio
          v-for="n in ['Ordered', 'Random']"
          :key="n"
          :label="n"
          :value="n"
        />
      </v-radio-group>
      <v-select
        v-model="timeLimit"
        :items="timeLimitOptions"
        label="Time Limit"
      />
    </v-card>
    <div class="game-options">
      <component
        :is="gameOptionComponent"
        v-if="config && gameOptionComponent"
        :config="config"
      />
    </div>
    <v-btn @click="createInvite()">
      Create Invite
    </v-btn>
  </div>
</template>
<script>
import supportedGames from "@/supportedGames"
import Socket from "@/socket"
import ConfigSetupGeneric from "@/components/games/ConfigSetupGeneric"

export default {
    name: "InviteCreateNew",
    props: ["gameType", "defaultConfig"],
    components: {
        ConfigSetupGeneric
    },
    data() {
        return {
            useDatabase: true,
            allowAnyoneJoin: false,
            timeLimit: 'No Limit',
            timeLimitOptions: ['No Limit'],
            playerOrder: 'Ordered',
            gameOptionComponent: supportedGames.games[this.gameType].configComponent || ConfigSetupGeneric,
            config: this.defaultConfig
        }
    },
    methods: {
        createInvite() {
            Socket.route(`invites/start`, { gameType: this.gameType, options: {}, gameOptions: this.config })
        }
    }
}
</script>
