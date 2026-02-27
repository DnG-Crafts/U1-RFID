import copy, os, logging

FILAMENT_LOAD_TEMP_UNKNOWN                      = 250
FILAMENT_UNLOAD_TEMP_UNKNOWN                    = 250
FILAMENT_CLEAN_NOZZLE_TEMP_UNKNOWN              = 170
FILAMENT_FLOW_TEMP_UNKNOWN                      = 220
FILAMENT_FLOW_K_UNKNOWN                         = 0.02
FILAMENT_FLOW_SLOW_V_UNKNOWN                    = 0.8
FILAMENT_FLOW_FAST_V_UNKNOWN                    = 8.0
FILAMENT_FLOW_K_MIN_UNKNOWN                     = 0.005
FILAMENT_FLOW_K_MAX_UNKNOWN                     = 0.065
FILAMENT_IS_SOFT_UNKNOWN                        = False
FILAMENT_PARAMETER_VERSION                      = '0.0.7'

FILAMENT_PARA_CFG_FILE                          = 'filament_parameters.json'
FILAMENT_PARA_CFG_DEFAULT = {
    'version': '0.0.6',
    'PLA': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 170,
                'is_soft': False,
                'flow_temp': 220,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.012,
                'flow_k_max': 0.028,
            },
        },
        'vendor_Snapmaker': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 170,
                'is_soft': False,
                'flow_temp': 220,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.012,
                'flow_k_max': 0.028,
            },
            'sub_Silk': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 180,
                'is_soft': False,
                'flow_temp': 230,
                'flow_k': 0.015,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.005,
                'flow_k_max': 0.025,
            },
        },
        'vendor_Polymaker': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 170,
                'is_soft': False,
                'flow_temp': 220,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.012,
                'flow_k_max': 0.028,
            },
            'sub_Silk': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 180,
                'is_soft': False,
                'flow_temp': 230,
                'flow_k': 0.015,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.005,
                'flow_k_max': 0.025,
            },
        },
    },
    'PLA-CF': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 170,
                'is_soft': False,
                'flow_temp': 220,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.012,
                'flow_k_max': 0.028,
            },
        },
    },
    'TPU': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 200,
                'is_soft': True,
                'flow_temp': 240,
                'flow_k': 0.28,
                'flow_slow_v': 0.4,
                'flow_fast_v': 1.8,
                'flow_k_min': 0.20,
                'flow_k_max': 0.36,
            },
        },
        'vendor_Snapmaker': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 200,
                'is_soft': True,
                'flow_temp': 240,
                'flow_k': 0.28,
                'flow_slow_v': 0.4,
                'flow_fast_v': 1.8,
                'flow_k_min': 0.20,
                'flow_k_max': 0.36,
            },
            'sub_95A HF': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 180,
                'is_soft': True,
                'flow_temp': 230,
                'flow_k': 0.23,
                'flow_slow_v': 0.4,
                'flow_fast_v': 1.8,
                'flow_k_min': 0.15,
                'flow_k_max': 0.31,
            }
        },
    },
    'PETG': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 200,
                'is_soft': False,
                'flow_temp': 255,
                'flow_k': 0.04,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.06,
            },
            'sub_HF': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 180,
                'is_soft': False,
                'flow_temp': 230,
                'flow_k': 0.025,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.013,
                'flow_k_max': 0.037,
            },
        },
        'vendor_Snapmaker': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 200,
                'is_soft': False,
                'flow_temp': 255,
                'flow_k': 0.05,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.038,
                'flow_k_max': 0.062,
            },
            'sub_HF': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 195,
                'is_soft': False,
                'flow_temp': 245,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.03,
            },
        },
        'vendor_Polymaker': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 200,
                'is_soft': False,
                'flow_temp': 255,
                'flow_k': 0.05,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.038,
                'flow_k_max': 0.062,
            },
            'sub_HF': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 195,
                'is_soft': False,
                'flow_temp': 245,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.03,
            },
        },
    },
    'PETG-CF': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 200,
                'is_soft': False,
                'flow_temp': 255,
                'flow_k': 0.025,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.013,
                'flow_k_max': 0.037,
            },
        },
    },
    'PETG-HF': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 180,
                'is_soft': False,
                'flow_temp': 230,
                'flow_k': 0.025,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.03,
            },
        },
    },
    'PCTG': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 200,
                'is_soft': False,
                'flow_temp': 255,
                'flow_k': 0.04,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.02,
                'flow_k_max': 0.06,
            },
        },
    },
    'EVA': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 160,
                'is_soft': True,
                'flow_temp': 210,
                'flow_k': 0.28,
                'flow_slow_v': 0.8,
                'flow_fast_v': 5.0,
                'flow_k_min': 0.20,
                'flow_k_max': 0.36,
            },
        },
    },
    'ABS': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 220,
                'is_soft': False,
                'flow_temp': 265,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.012,
                'flow_k_max': 0.028,
            },
        },
        'vendor_Snapmaker': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 220,
                'is_soft': False,
                'flow_temp': 265,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.005,
                'flow_k_max': 0.065,
            },
        },
        'vendor_Polymaker': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 220,
                'is_soft': False,
                'flow_temp': 265,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.012,
                'flow_k_max': 0.028,
            },
        },
    },
    'ASA': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 250,
                'unload_temp': 250,
                'clean_nozzle_temp': 220,
                'is_soft': False,
                'flow_temp': 260,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.012,
                'flow_k_max': 0.028,
            },
        },
    },
    'PA': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 300,
                'unload_temp': 300,
                'clean_nozzle_temp': 220,
                'is_soft': False,
                'flow_temp': 250,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.028,
            },
        },
    },
    'PA-CF': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 300,
                'unload_temp': 300,
                'clean_nozzle_temp': 240,
                'is_soft': False,
                'flow_temp': 250,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.028,
            },
        },
    },
    'PA6-CF': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 300,
                'unload_temp': 300,
                'clean_nozzle_temp': 240,
                'is_soft': False,
                'flow_temp': 250,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.028,
            },
        },
    },
    'PA-GF': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 300,
                'unload_temp': 300,
                'clean_nozzle_temp': 240,
                'is_soft': False,
                'flow_temp': 250,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.028,
            },
        },
    },
    'PA6-GF': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 300,
                'unload_temp': 300,
                'clean_nozzle_temp': 240,
                'is_soft': False,
                'flow_temp': 250,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.01,
                'flow_k_max': 0.028,
            },
        },
    },
    'PC': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 300,
                'unload_temp': 300,
                'clean_nozzle_temp': 220,
                'is_soft': False,
                'flow_temp': 280,
                'flow_k': 0.025,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.017,
                'flow_k_max': 0.033,
            },
        },
    },
    'PC-ABS': {
        'vendor_generic': {
            'sub_generic': {
                'load_temp': 300,
                'unload_temp': 300,
                'clean_nozzle_temp': 220,
                'is_soft': False,
                'flow_temp': 270,
                'flow_k': 0.02,
                'flow_slow_v': 0.8,
                'flow_fast_v': 8.0,
                'flow_k_min': 0.005,
                'flow_k_max': 0.065,
            },
        },
    },
}

FILAMENT_PARA_CFG_UNKNOWN = {
    'load_temp': FILAMENT_LOAD_TEMP_UNKNOWN,
    'unload_temp': FILAMENT_UNLOAD_TEMP_UNKNOWN,
    'clean_nozzle_temp': FILAMENT_CLEAN_NOZZLE_TEMP_UNKNOWN,
    'is_soft': FILAMENT_IS_SOFT_UNKNOWN,
    'flow_temp': FILAMENT_FLOW_TEMP_UNKNOWN,
    'flow_k': FILAMENT_FLOW_K_UNKNOWN,
    'flow_slow_v': FILAMENT_FLOW_SLOW_V_UNKNOWN,
    'flow_fast_v': FILAMENT_FLOW_FAST_V_UNKNOWN,
    'flow_k_min': FILAMENT_FLOW_K_MIN_UNKNOWN,
    'flow_k_max': FILAMENT_FLOW_K_MAX_UNKNOWN,
}

class FilamentParameters:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.reactor = self.printer.get_reactor()
        config_dir = self.printer.get_snapmaker_config_dir()
        config_name = FILAMENT_PARA_CFG_FILE
        self._config_path = os.path.join(config_dir, config_name)
        self._config = self.printer.load_snapmaker_config_file(
                            self._config_path,
                            FILAMENT_PARA_CFG_DEFAULT,
                            create_if_not_exist=True)

        gcode = self.printer.lookup_object('gcode')
        gcode.register_command('FILAMENT_PARA_GET_ALL_INFO',
                               self.cmd_FILAMENT_PARA_GET_ALL_INFO)
        self.printer.register_event_handler("klippy:ready", self._ready)

    def _ready(self):
        version = self._config.get('version', None)
        if version != FILAMENT_PARAMETER_VERSION:
            self.reset_parameters()


    def get_status(self, eventtime=None):
        return copy.deepcopy(self._config)

    def get_filament_parameters(self, filament_vendor, filament_main_type, filament_sub_type):
        main_type = None
        vendor = None
        sub_type = None
        try:
            main_type = self._config.get(filament_main_type)
            vendor = main_type.get('vendor_' + filament_vendor, None)
            if vendor == None:
                vendor = main_type.get('vendor_generic')
            sub_type = vendor.get('sub_' + filament_sub_type, None)
            if sub_type == None:
                sub_type = vendor.get('sub_generic')
        except:
            pass

        if main_type == None or vendor == None or sub_type == None:
            return FILAMENT_PARA_CFG_UNKNOWN
        else:
            return sub_type
    def get_load_temp(self, filament_vendor, filament_main_type, filament_sub_type):
        parameter = self.get_filament_parameters(filament_vendor, filament_main_type, filament_sub_type)
        return parameter.get('load_temp', FILAMENT_LOAD_TEMP_UNKNOWN)

    def get_unload_temp(self, filament_vendor, filament_main_type, filament_sub_type):
        parameter = self.get_filament_parameters(filament_vendor, filament_main_type, filament_sub_type)
        return parameter.get('unload_temp', FILAMENT_UNLOAD_TEMP_UNKNOWN)

    def get_clean_nozzle_temp(self, filament_vendor, filament_main_type, filament_sub_type):
        parameter = self.get_filament_parameters(filament_vendor, filament_main_type, filament_sub_type)
        return parameter.get('clean_nozzle_temp', FILAMENT_CLEAN_NOZZLE_TEMP_UNKNOWN)

    def get_flow_temp(self, filament_vendor, filament_main_type, filament_sub_type):
        parameter = self.get_filament_parameters(filament_vendor, filament_main_type, filament_sub_type)
        return parameter.get('flow_temp', FILAMENT_FLOW_TEMP_UNKNOWN)

    def get_flow_k(self, filament_vendor, filament_main_type, filament_sub_type):
        parameter = self.get_filament_parameters(filament_vendor, filament_main_type, filament_sub_type)
        return parameter.get('flow_k', FILAMENT_FLOW_K_UNKNOWN)

    def get_is_soft(self, filament_vendor, filament_main_type, filament_sub_type):
        parameter = self.get_filament_parameters(filament_vendor, filament_main_type, filament_sub_type)
        return parameter.get('is_soft', FILAMENT_IS_SOFT_UNKNOWN)

    def reset_parameters(self):
        self._config = copy.deepcopy(FILAMENT_PARA_CFG_DEFAULT)
        self._config['version'] = FILAMENT_PARAMETER_VERSION
        self.printer.update_snapmaker_config_file(self._config_path, self._config, FILAMENT_PARA_CFG_DEFAULT)
        logging.info("[filament_parameters] reset filament parameters")

    def cmd_FILAMENT_PARA_GET_ALL_INFO(self, gcmd):
        gcmd.respond_info(str(self._config))

def load_config(config):
    return FilamentParameters(config)

